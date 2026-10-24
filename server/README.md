# QuickTix Server

The backend REST API service for [QuickTix](../README.md), implemented with [Java 25](https://openjdk.org/projects/jdk/25/) and [Spring Boot 4.0](https://spring.io/projects/spring-boot).

The service handles authentication, venue administration, event publishing, automatic seat inventory generation, and transactional seat bookings with optimistic locking concurrency controls to guarantee that seats can never be double-booked.

**Interactive API Documentation:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
**Raw OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

<!-- SCREENSHOT: Swagger UI interactive dashboard at http://localhost:8080/swagger-ui.html with Authorize bearer dialog open -->

---

## Architectural Design

The backend is organized into decoupled layers where each layer has a single responsibility:


```
[ HTTP Requests ]
       │
       ▼
[ Security Filter Chain: JwtAuthFilter & CorsFilter ]
       │  (Validates In-Memory Bearer JWT, handles CORS dynamically via YAML)
       ▼
[ Controller Layer: @RestController ]
       │  (DTO validation with Jakarta @Valid, Swagger annotations, HttpOnly cookies)
       ▼
[ Service Layer: @Service & Spring Cache ]
       │  (Business rules, ownership checks, @Transactional boundaries, @Cacheable / @CacheEvict)
       ├─────────────────────────────┐
       │ (Cache Miss / Writes)       │ (Hot Read Paths)
       ▼                             ▼
[ Repository Layer: Spring Data JPA ]  [ Redis 7 In-Memory Cache ]
       │  (Optimistic @Version checks) (Port 6379, 5-min TTL safety net)
       ▼
[ PostgreSQL 16 Database ]
(Port 5432, ACID persistence)
```

### Package Layout
```
com.asyncsourav.quicktix/
├── config/           # SecurityConfig, CorsProperties, RedisConfig, OpenApiConfig, DataInitializer
├── controller/       # Auth, Event, Venue, Booking, Admin, Ping controllers
├── dto/
│   ├── common/       # ApiResponse<T>, ErrorResponse
│   ├── request/      # RegisterRequest, LoginRequest, RefreshTokenRequest, EventRequest, BookingRequest
│   └── response/     # AuthResponse, UserResponse, EventResponse, BookingResponse, StatsResponse
├── entity/           # User, Venue, Event, Seat (@Version), Booking, RefreshToken
├── exception/        # Custom exceptions & GlobalExceptionHandler (@RestControllerAdvice)
├── repository/       # UserRepository, EventRepository, SeatRepository, BookingRepository, RefreshTokenRepository
├── security/         # JwtUtil, JwtAuthFilter, CustomUserDetailsService
└── service/          # AuthService, EventService, VenueService, BookingService, AdminService
```

---

## Concurrency Control & Double-Booking Prevention

### The Race Condition Problem
When tickets for popular events go on sale, thousands of concurrent requests attempt to reserve the same seat within the same millisecond. Simple application-level validation (`if (seat.getStatus() == SeatStatus.AVAILABLE)`) fails because competing threads read `AVAILABLE` concurrently before any transaction commits.

### The Solution: Optimistic Locking with `@Version`

1. **Entity Version Column**:
   In [Seat.java](src/main/java/com/asyncsourav/quicktix/entity/Seat.java):
   ```java
   @Entity
   @Table(name = "seats", uniqueConstraints = {
       @UniqueConstraint(name = "uk_event_seat_label", columnNames = {"event_id", "seat_label"})
   })
   public class Seat {
       // ...
       @Enumerated(EnumType.STRING)
       @Column(nullable = false)
       private SeatStatus status;

       @Version
       @Column(nullable = false)
       private Long version;
   }
   ```

2. **Atomic Batch Reservation**:
   In `BookingService.createBooking`, the operation runs inside a `@Transactional` boundary.

3. **Database-Level Predicate**:
   When updating the seat, Hibernate generates an update query conditioned on the version:
   ```sql
   UPDATE seats 
   SET status = 'BOOKED', booking_id = ?, version = 2 
   WHERE id = 3 AND version = 1;
   ```

4. **Collision Handling**:
   * The first transaction to commit successfully updates the row and increments `version` to `2`.
   * The competing transaction executes with `version = 1`. Because the row is already at version `2`, zero rows are updated.
   * Hibernate detects zero updated rows and throws an `ObjectOptimisticLockingFailureException`.
   * The transaction rolls back all seat updates in the batch atomically.

5. **HTTP Translation**:
   In [GlobalExceptionHandler.java](src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java), the exception is mapped to HTTP 409 Conflict:
   ```json
   {
     "timestamp": "2026-10-21T18:30:00.124",
     "status": 409,
     "error": "Conflict",
     "message": "This seat was just selected or booked by another user. Please choose an available seat.",
     "path": "/api/bookings"
   }
   ```

<!-- SCREENSHOT: Node.js race condition script output showing 10 parallel requests with 1 winner (201) and 9 conflict responses (409) -->

---

## Security Architecture: Dual Token Rotation & Reuse Detection

QuickTix implements **Rotating Refresh Tokens with Session Family Tracking**:

### 1. Data Storage ([RefreshToken.java](src/main/java/com/asyncsourav/quicktix/entity/RefreshToken.java))
Refresh tokens are never stored in plaintext. When generated, only the **SHA-256 hash** is saved in PostgreSQL:

| Field | Type | Purpose |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Primary Key |
| `user_id` | `BIGINT` | Foreign Key to User |
| `token_hash` | `VARCHAR(64)` | SHA-256 hex string with Unique Index |
| `family_id` | `VARCHAR(64)` | UUID grouping tokens within the same session family |
| `used` | `BOOLEAN` | Marked `true` after single-use rotation |
| `revoked` | `BOOLEAN` | Marked `true` if logged out or if reuse is detected |
| `expires_at` | `TIMESTAMP` | Expiration timestamp (7 days) |

### 2. Token Lifecycle & Rotation
* **Login (`POST /api/auth/login`)**: Validates credentials, issues a short-lived access token (15 mins) in JSON and sets the rotating refresh token (7 days) in a Secure, HttpOnly cookie (`Path=/api/auth; SameSite=Lax`).
* **Rotation (`POST /api/auth/refresh`)**:
  1. Reads refresh token from the HttpOnly cookie (or optional fallback JSON body).
  2. Hashes incoming token and looks up entity.
  3. Verifies token is not expired.
  4. Marks current token as `used = true`.
  5. Generates a new access token in JSON and a new refresh token in a rotated HttpOnly cookie under the **same `family_id`**.
* **Reuse Detection (Replay Defense)**:
  If a token with `used == true` or `revoked == true` is presented:
  ```java
  if (token.isUsed() || token.isRevoked()) {
      refreshTokenRepository.revokeFamily(token.getFamilyId());
      throw new BadCredentialsException("Security violation: Refresh token reuse detected.");
  }
  ```
  The server immediately revokes every token in that `family_id` in PostgreSQL and wipes the cookie from the browser (`Max-Age=0`).
* **Logout (`POST /api/auth/logout`)**: Revokes the active session family in PostgreSQL and clears the HttpOnly cookie.

### 3. Automated Test Verification
Verified by unit tests in [AuthServiceTest.java](src/test/java/com/asyncsourav/quicktix/service/AuthServiceTest.java):
* `login_ShouldIssueDualTokensAndPersistHashedRefreshToken` - PASSED
* `refresh_ShouldRotateTokensWithinSameFamily` - PASSED
* `refresh_WhenTokenIsAlreadyUsed_ShouldTriggerReuseDetectionAndRevokeFamily` - PASSED
* `refresh_WhenTokenIsRevoked_ShouldTriggerReuseDetectionAndRevokeFamily` - PASSED
* `refresh_WhenTokenIsExpired_ShouldThrowBadCredentialsException` - PASSED
* `logout_ShouldRevokeSessionFamily` - PASSED

---

## Distributed Caching with Redis 7

To accelerate catalog discovery and protect PostgreSQL during high-traffic ticket drops, QuickTix implements distributed in-memory caching via Redis 7:

### 1. Cache Annotations & Strategy
* **Event Listing (`GET /api/events`)**:
  `@Cacheable(value = "events", key = "'all:' + #category + ':' + #search")` caches serialized JSON payloads in Redis.
* **Event Detail (`GET /api/events/{id}`)**:
  `@Cacheable(value = "event", key = "#id")` caches individual event metadata.
* **Cache Invalidation (`@CacheEvict`)**:
  Whenever an event is created, modified, or deleted (`createEvent`, `updateEvent`, `deleteEvent`), `@CacheEvict(value = {"events", "event"}, allEntries = true)` purges the cached entries immediately to ensure strict consistency.

### 2. TTL Safety Configuration
Configured in [RedisConfig.java](src/main/java/com/asyncsourav/quicktix/config/RedisConfig.java):
```java
RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
```
A default **5-minute TTL** acts as a safety boundary for background expiration. If `SPRING_CACHE_TYPE: none` is set, Redis caching is bypassed cleanly without code changes.

### 3. Empirical Latency Benchmark (1,000 Seeded Events)
Measured using `resources/latency.mjs` (200 sequential requests, discarding the first 20 as warm-up):
* **`GET /api/events` (1,000 items)**:
  * Run A (Postgres Only): avg 24.7 ms | p50 22.1 ms | p95 38.9 ms
  * Run B (Redis 7 Cache): avg 18.4 ms (25.5% faster) | p50 17.7 ms (19.9% faster) | p95 31.2 ms
* **`GET /api/events/1` (Detail view)**:
  * Run A (Postgres Only): avg 15.2 ms | p50 15.3 ms | p95 21.8 ms
  * Run B (Redis 7 Cache): avg 13.7 ms (10.5% faster) | p50 13.7 ms (10.5% faster) | p95 20.4 ms

---

## Distributed Rate Limiting: Bucket4j + Redis

To protect the booking endpoint (`POST /api/bookings`) against automated ticket-scalping bots during high-traffic ticket drops, QuickTix integrates **Bucket4j** backed by **Lettuce & Redis 7**:

### 1. The Scalper Bot Threat & Domain Rationale
During ticket drops, scalpers run parallelized scripts hitting the booking endpoint dozens of times per second. While PostgreSQL optimistic concurrency control (`@Version`) prevents duplicate bookings, thousands of rogue bot requests bombard the database with transaction rollbacks, depleting HikariCP connection pools and starving legitimate users. Rate limiting at the controller boundary throttles bot traffic before it touches database transactions.

### 2. Dual-Layer Token Bucket Limits
* **Per-User Limit**:
  * **Capacity (Burst)**: 5 tokens
  * **Refill Rate**: 1 token per second (`refillGreedy`)
  * **Key**: `rate:booking:user:{username}`
* **Per-IP Limit**:
  * **Capacity (Burst)**: 10 tokens
  * **Refill Rate**: 2 tokens per second (`refillGreedy`)
  * **Key**: `rate:booking:ip:{clientIp}` (handles multi-account bot floods originating from a single IP/proxy)

### 3. Fail-Open Architecture & Outage Resiliency
* **Interview Insight**: *Rate limiting protects system capacity, but it is not what guarantees booking correctness. That is the job of PostgreSQL optimistic locking.*
* If Redis experiences an outage or transient network partition, the rate limiter **fails open**:
  ```java
  ConsumptionProbe probe;
  try {
      probe = bucket.tryConsumeAndReturnRemaining(1);
  } catch (Exception e) {
      log.warn("Rate limiter unavailable, failing open: {}", e.toString());
      return true; // allow request through to Postgres
  }
  if (!probe.isConsumed()) {
      long wait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
      throw new RateLimitExceededException(wait);
  }
  return true;
  ```
* **Short Timeouts (200ms)**: Configured via `spring.data.redis.timeout: 200ms` and `spring.data.redis.connect-timeout: 200ms` to prevent slow Redis calls from stalling user threads.
* **Cache Error Fallback ([CacheErrorConfig.java](src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java))**: Implements `CachingConfigurer.errorHandler()` so that if Redis is down, `@Cacheable` queries on `GET /api/events` transparently fall back to PostgreSQL instead of throwing HTTP 500.

### 4. HTTP 429 Response Envelope
When rate limits are exceeded, [GlobalExceptionHandler.java](src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java) returns standard HTTP 429 with `Retry-After` headers:
```http
HTTP/1.1 429 Too Many Requests
Retry-After: 3
X-Rate-Limit-Retry-After-Seconds: 3
Content-Type: application/json

{
  "timestamp": "2026-10-24T08:15:00.124",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many booking requests. Please wait 3 seconds before trying again.",
  "path": "/api/bookings"
}
```

---

## Production Multi-Stage Containerization

The backend utilizes an optimized multi-stage [Dockerfile](Dockerfile):

```dockerfile
# Stage 1: Build stage with compiler and dependencies
FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline  # Caches Maven dependencies in Docker layer
COPY src ./src
RUN mvn clean package -DskipTests  

# Stage 2: Minimal runtime image containing JRE only
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Measured Image Size Benchmark
```text
IMAGE             TAG      DISK USAGE   CONTENT SIZE
quicktix          single   870 MB       310 MB
quicktix          multi    434 MB       137 MB
```
* **Disk Usage Reduction:** **50.11%** (from 870 MB down to 434 MB)
* **Content Layer Reduction:** **55.81%** (from 310 MB down to 137 MB)

---

## Getting Started

### Option A: Run Full Stack via Docker Compose (Recommended)
From the repository root:
```bash
docker compose up -d --build
```
* Boots PostgreSQL 16 on port `5432` with healthcheck.
* Boots Redis 7 on port `6379` with healthcheck.
* Builds and starts Spring Boot backend on port `8080`.
* Automatically seeds demo data on first start.

To view logs:
```bash
docker logs -f quicktix-backend
```

### Option B: Local IDE Development
When actively developing and debugging in IntelliJ IDEA:

1. Start PostgreSQL and Redis containers:
   ```bash
   docker compose up -d postgres redis
   ```
2. Run the application from your IDE or via Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## Configuration Reference

Configuration is managed across `application.yml`, `application-dev.yml`, and `application-prod.yml`:

| Environment Variable | Description | Default (Dev) | Default (Prod) |
| :--- | :--- | :--- | :--- |
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/quicktix_db?options=-c%20timezone%3DUTC` | `jdbc:postgresql://postgres:5432/quicktix_db?options=-c%20timezone%3DUTC` |
| `DB_USERNAME` | Database username | `postgres` | `${DB_USERNAME}` |
| `DB_PASSWORD` | Database password | `postgres` | `${DB_PASSWORD}` |
| `REDIS_HOST` | Redis cache hostname | `localhost` | `redis` |
| `REDIS_PORT` | Redis cache port | `6379` | `6379` |
| `JWT_SECRET` | 256-bit key for HS256 token signing | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | `${JWT_SECRET}` |
| `JWT_ACCESS_EXPIRATION_MS` | Access token lifetime (ms) | `900000` (15 mins) | `900000` (15 mins) |
| `JWT_REFRESH_EXPIRATION_MS` | Refresh token lifetime (ms) | `604800000` (7 days) | `604800000` (7 days) |
| `JWT_COOKIE_SECURE` | HttpOnly cookie Secure flag | `false` | `false` (set `true` with HTTPS) |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origins (comma-separated) | `http://localhost:5173,http://localhost:3000` | `${CORS_ALLOWED_ORIGINS}` |
| `PORT` | HTTP server listening port | `8080` | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` | `prod` |

---

## Response Contracts

### Success Response Envelope (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Booking confirmed successfully",
  "data": {
    "id": 1,
    "userId": 3,
    "userName": "Sourav (Attendee)",
    "eventId": 1,
    "eventTitle": "Coldplay: Music of the Spheres World Tour",
    "seats": [
      {
        "id": 3,
        "eventId": 1,
        "seatLabel": "A3",
        "price": 89.99,
        "status": "BOOKED"
      }
    ],
    "totalAmount": 89.99,
    "status": "CONFIRMED",
    "createdAt": "2026-10-21T18:30:00"
  },
  "timestamp": "2026-10-21T18:30:00.125"
}
```

### Error Response Envelope (`ErrorResponse`)
```json
{
  "timestamp": "2026-10-21T18:30:00.412",
  "status": 409,
  "error": "Conflict",
  "message": "This seat was just selected or booked by another user. Please choose an available seat.",
  "path": "/api/bookings"
}
```

---

## Interactive API Documentation (Swagger UI)

With the backend running:
* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3.0 JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To test authenticated endpoints in Swagger UI:
1. Call `POST /api/auth/login` with demo credentials (`user@quicktix.com` / `password123`).
2. Copy `data.token` from the response.
3. Click the **Authorize** lock button in the top right of the Swagger UI interface.
4. Paste the token into the value field and click **Authorize**.

---

## Author

**Sourav Kumar**
* **GitHub:** [https://github.com/asyncsourav](https://github.com/asyncsourav)
* **LinkedIn:** [https://www.linkedin.com/in/asyncsourav](https://www.linkedin.com/in/asyncsourav)
* **LeetCode:** [https://leetcode.com/u/asyncsourav](https://leetcode.com/u/asyncsourav)
* **Email:** [xsouravkumar357@gmail.com](mailto:xsouravkumar357@gmail.com)
