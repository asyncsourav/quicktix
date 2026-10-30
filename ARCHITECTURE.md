# QuickTix System Architecture and Request Lifecycle

This document provides a technical walkthrough of how requests traverse the QuickTix platform, covering security, rate limiting, caching, concurrency control, data persistence, and real-time broadcasting.

For a file-by-file directory map of the codebase, see [folder-structure.md](folder-structure.md).

---

## 1. High-Level Architecture Overview

QuickTix is designed around a multi-tier, decoupled architecture:

- **Frontend Client**: React 19 single-page application (SPA) bundled with Vite, maintaining access tokens in JavaScript memory and listening for real-time seat status via STOMP over WebSocket.
- **Backend Service**: Java 25 / Spring Boot 4 REST API running on Tomcat 11, implementing stateless authentication, distributed rate limiting, and JPA persistence.
- **Data Tier**: PostgreSQL 16 database utilizing ACID transactions and Hibernate optimistic locking (`@Version`) for race condition safety.
- **Cache & Throttling Tier**: Redis 7 running cache-aside caching for catalog reads and Bucket4j token bucket state for distributed rate limiting.
- **Messaging Broker**: Spring WebSocket in-memory STOMP broker broadcasting seat mutations to subscribed clients on `/topic/event.{id}`.

---

## 2. End-to-End Request Lifecycle

When an HTTP request enters the backend (for example, `POST /api/bookings` or `GET /api/events`), it follows a deterministic 10-stage execution pipeline.

```
[Client Request]
       │
       ▼
1. Servlet Container (Tomcat 11) & CORS Filter
       │
       ▼
2. Security Filter Chain (JwtAuthFilter)
   ├── Validate Bearer Header
   ├── Verify HMAC-SHA256 Signature
   └── Populate SecurityContextHolder with UserDetails
       │
       ▼
3. Distributed Rate Limiter & Circuit Breaker (Bucket4j + Resilience4j)
   ├── Check IP Token Bucket (Redis)
   ├── Check User Token Bucket (Redis)
   └── Fail-Open bypass if Redis Circuit Breaker trips OPEN
       │
       ▼
4. DispatcherServlet & Request Mapping
       │
       ▼
5. Controller Layer (Jakarta Validation)
   ├── Validate DTO (@Valid BookingRequest)
   └── Extract authenticated user (@AuthenticationPrincipal)
       │
       ▼
6. Service Layer & Cache-Aside Evaluation
   ├── Read Path: Check Redis Cache -> Return hit or query DB & populate cache
   └── Write Path: Evict cached entries & proceed to transactional business logic
       │
       ▼
7. Database Transaction & Optimistic Locking (@Transactional)
   ├── Verify seat belongs to event and status is AVAILABLE
   ├── Commit transaction -> Hibernate checks @Version column
   └── Conflict -> OptimisticLockException -> 409 Conflict rollback
       │
       ▼
8. Real-Time WebSocket Broadcasting (STOMP)
   └── Publish SeatStatusUpdate payload to /topic/event.{eventId}
       │
       ▼
9. Asynchronous Notification (@Async Thread Pool)
   └── Submit booking confirmation email task off the request path
       │
       ▼
10. Global Exception Handling & Response Serialization
   └── Envelope response in ApiResponse<T> or ErrorResponse JSON
```

---

## 3. Stage-by-Stage Technical Breakdown

### Stage 1: Ingestion & CORS Verification
- **Where**: `CorsProperties`, `SecurityConfig`.
- **What happens**: The incoming HTTP request is intercepted by the servlet filter chain. The `CorsFilter` validates the `Origin` header against permitted domains configured in `CORS_ALLOWED_ORIGINS`.
- **Why**: Protects cross-origin requests between the deployed frontend (Vercel) and backend (Render), ensuring cookies and authorization headers are only accepted from authorized domains.

### Stage 2: JWT Authentication and Security Context
- **Where**: `JwtAuthFilter`, `JwtUtil`, `CustomUserDetailsService`.
- **What happens**:
  1. The filter checks for the `Authorization: Bearer <token>` header.
  2. If present, `JwtUtil` validates the token's HMAC-SHA256 signature against `JWT_SECRET` and ensures the token has not expired.
  3. The username (email) and authorities (`ROLE_USER`, `ROLE_ORGANIZER`, `ROLE_ADMIN`) are extracted.
  4. A `UsernamePasswordAuthenticationToken` is created and stored in Spring's thread-local `SecurityContextHolder`.
  5. If the token is missing on protected endpoints, Spring Security halts execution and issues an HTTP 401 Unauthorized response before hitting any controller.
- **Why**: Decouples authentication verification from business logic, ensuring downstream controllers and services can rely securely on `@AuthenticationPrincipal`.

### Stage 3: Distributed Rate Limiting & Resilience4j Circuit Breaker
- **Where**: `RateLimiterService`, `RateLimiterConfig`.
- **What happens**:
  1. For critical endpoints (such as ticket booking), the request client IP and username are extracted.
  2. Distributed token buckets stored in Redis are evaluated via Lettuce and Bucket4j:
     - **Per-IP limit**: Burst capacity of 10 tokens, refill rate of 2 tokens per second.
     - **Per-User limit**: Burst capacity of 5 tokens, refill rate of 1 token per second.
  3. If insufficient tokens exist, `RateLimitExceededException` is thrown, producing an HTTP 429 response with a `Retry-After` header.
  4. **Resilience4j Circuit Breaker**: Wraps the Redis connection. If Redis fails 5 consecutive times, the breaker transitions to `OPEN` for 15 seconds, instantly bypassing rate-limiting checks without introducing socket latency.
- **Why**: Prevents bot-driven ticket scalping and brute-force reservation attempts while ensuring database availability is never compromised if Redis becomes unreachable.

### Stage 4: Controller Dispatching and Payload Validation
- **Where**: `BookingController`, `EventController`, `AuthController`.
- **What happens**:
  1. `DispatcherServlet` routes the request to the matching `@PostMapping` or `@GetMapping` handler.
  2. Jakarta Validation annotations (`@Valid`, `@NotNull`, `@NotEmpty`) execute automatically against the incoming request record (such as `BookingRequest`).
  3. If payload validation fails, a `MethodArgumentNotValidException` is intercepted by `GlobalExceptionHandler`, returning an HTTP 400 Bad Request with field-level error messages.
- **Why**: Guarantees malformed or invalid inputs are rejected at the edge before allocating database connections or initiating transactions.

### Stage 5: Service Layer & Redis Cache-Aside Pattern
- **Where**: `EventService`, `RedisConfig`, `CacheErrorConfig`.
- **What happens**:
  1. **Read Operations** (`GET /api/events`, `GET /api/events/{id}`): Spring Cache intercepts calls annotated with `@Cacheable`.
     - *Cache Hit*: Redis returns serialized JSON data directly. Method execution completes in < 10 ms without querying PostgreSQL.
     - *Cache Miss*: The database query executes via JPA, the result is serialized into Redis with a 5-minute TTL, and the payload is returned.
  2. **Write Operations** (Event creation, modification, deletion): Trigger `@CacheEvict(value = "events", allEntries = true)`.
  3. **Fault-Tolerant Cache Errors**: If Redis is offline, `CacheErrorConfig` catches serialization or connection errors, logs a warning, and falls back directly to PostgreSQL without failing the user's request.
- **Why**: Accelerates catalog read operations by ~75% while maintaining absolute correctness and graceful degradation.

### Stage 6: Database Transactions & Optimistic Locking Concurrency
- **Where**: `BookingService`, `SeatRepository`, PostgreSQL database.
- **What happens**:
  1. Method runs within a `@Transactional` boundary.
  2. Seats requested for booking are queried from the database. The service verifies:
     - All seat IDs exist.
     - All seats belong to the specified target event.
     - Every seat is currently in `AVAILABLE` status.
  3. Seat status is mutated to `BOOKED` and linked to the new `Booking` entity.
  4. **Optimistic Locking**: The `Seat` entity contains a `@Version Long version` column. When Hibernate issues the SQL update (`UPDATE seats SET status = 'BOOKED', version = version + 1 WHERE id = ? AND version = ?`), it checks the version match.
  5. If another user completed a booking on any of the same seats milliseconds earlier, the version count will not match, Hibernate throws `OptimisticLockException`, the entire transaction rolls back, and the client receives an HTTP 409 Conflict.
- **Why**: Eliminates double-booking race conditions without heavyweight pessimistic table locks, maximizing system throughput during ticket sales.

### Stage 7: Real-Time State Propagation via WebSocket
- **Where**: `SeatBroadcastService`, `WebSocketConfig`.
- **What happens**:
  1. Once the booking transaction commits successfully, `SeatBroadcastService` is invoked.
  2. A `SeatStatusUpdate` DTO payload is constructed containing the seat ID, label, and new status (`BOOKED` or `AVAILABLE`).
  3. The message is pushed to Spring's message broker at destination `/topic/event.{eventId}`.
  4. Connected browser clients subscribed to that STOMP topic receive the message within ~25 ms and update their seat picker UI in real time without refreshing.
- **Why**: Provides instantaneous feedback to all active shoppers, preventing users from attempting to select seats that have already been booked.

### Stage 8: Asynchronous Email Notification
- **Where**: `EmailService`, `AsyncConfig`.
- **What happens**:
  1. Confirmation email generation is annotated with `@Async("taskExecutor")`.
  2. The task is dispatched to a dedicated thread pool executor (`ThreadPoolTaskExecutor`) decoupled from Tomcat's HTTP worker threads.
  3. Detached DTOs (`BookingConfirmationEmailDetails`) are passed to prevent holding open database connections or Hibernate sessions.
- **Why**: Ensures slow third-party SMTP server latency never impacts the HTTP response time experienced by the booking user.

### Stage 9: Token Family Rotation and Reuse Detection
- **Where**: `AuthService`, `RefreshTokenRepository`.
- **What happens**:
  1. Access tokens expire after 15 minutes and live solely in frontend JavaScript memory.
  2. The refresh token (7-day validity) is stored as an HTTP-only, secure cookie. The database stores only the SHA-256 hash of the token along with a `familyId` UUID.
  3. During `POST /api/auth/refresh`, an atomic database update queries `WHERE token_hash = ? AND used = false`.
  4. If valid, the old token is marked `used = true`, and a new refresh token is issued within the same `familyId`.
  5. **Reuse Detection**: If an already-used refresh token is presented (indicating potential theft), all tokens sharing that `familyId` are immediately revoked via `@Transactional(noRollbackFor = BadCredentialsException.class)`.
- **Why**: Thwarts token replay attacks and guarantees automated revocation if credentials are compromised.

### Stage 10: Standardized Response Serialization
- **Where**: `GlobalExceptionHandler`, `ApiResponse`, Jackson serializer.
- **What happens**:
  - Successful responses are wrapped in `ApiResponse<T>` containing `success: true`, message, data payload, and server timestamp.
  - Failures are routed to `GlobalExceptionHandler`, producing structured JSON error responses with standard HTTP status codes:
    - `400`: Validation or bad request error.
    - `401`: Missing or invalid credentials.
    - `403`: Insufficient role permissions.
    - `404`: Requested resource not found.
    - `409`: Concurrency conflict or double-booking attempt.
    - `429`: Rate limit exceeded with retry countdown.
    - `500`: Unhandled internal server error.
- **Why**: Ensures uniform, predictable client integration and simplifies error handling across all frontend pages.
