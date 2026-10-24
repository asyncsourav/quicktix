# End-to-End System Architecture & Request Lifecycle

This document provides a comprehensive, step-by-step trace of QuickTix's request lifecycle across the frontend, security filters, rate limiters, caching layers, business logic, and transactional database persistence.

---

## High-Level Request Pipeline

```
[ Browser / Frontend Client ]
             │
             │ HTTPS (In-Memory Bearer Token + HttpOnly Cookie)
             ▼
[ 1. Spring Security Filter Chain ]
     ├── CorsFilter (Dynamic YAML origin verification)
     └── JwtAuthFilter (Stateless Bearer JWT validation & UserDetails extraction)
             │
             ▼
[ 2. Web Layer & Validation ]
     ├── DTO Binding & Validation (@Valid Jakarta constraints)
     └── Client IP Extraction (X-Forwarded-For, X-Real-IP, RemoteAddr)
             │
             ▼
[ 3. Anti-Scalping Rate Limiter (Bucket4j + Redis 7) ]
     ├── Per-IP Limit Check (rate:booking:ip:{clientIp})
     ├── Per-User Limit Check (rate:booking:user:{username})
     └── Fail-Open Try/Catch Guard (Bypasses limiter if Redis is unavailable)
             │
             ▼
[ 4. Service & Caching Layer ]
     ├── Catalog Reads: Spring Cache (@Cacheable hot paths in Redis)
     │     └── CacheErrorConfig (Falls back to PostgreSQL if Redis times out)
     └── Writes / Bookings: @Transactional boundaries
             │
             ▼
[ 5. Persistence & Concurrency Control ]
     ├── Optimistic Locking: Hibernate @Version check on Seat
     ├── PostgreSQL 16: ACID commit with unique constraints
     └── Cache Invalidation: @CacheEvict purges stale catalog entries
             │
             ▼
[ 6. Response Dispatch ]
     ├── HTTP 200 / 201: ApiResponse<T> JSON envelope
     ├── HTTP 409: Seat conflict (ObjectOptimisticLockingFailureException)
     └── HTTP 429: Rate limit exceeded (Retry-After header attached)
```

---

## Detailed Lifecycle 1: Live Event Catalog (`GET /api/events`)

The event catalog read path is optimized for high throughput using distributed in-memory caching.

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (Browser)
    participant Ctrl as EventController
    participant Svc as EventService
    participant Cache as Redis 7 (Spring Cache)
    participant DB as PostgreSQL 16

    User->>Ctrl: GET /api/events?category=Concert&search=Coldplay
    Ctrl->>Svc: getAllEvents(category, search)
    
    alt Cache Hit
        Svc->>Cache: Lookup key "events::all:Concert:Coldplay"
        Cache-->>Svc: Return cached serialized JSON
        Svc-->>Ctrl: List<EventResponse>
        Ctrl-->>User: HTTP 200 OK (from Redis in ~18ms)
    else Cache Miss (or First Request)
        Svc->>Cache: Lookup key "events::all:Concert:Coldplay"
        Cache-->>Svc: Key not found
        Svc->>DB: SELECT * FROM events e WHERE ... ORDER BY start_time ASC
        DB-->>Svc: Event records
        Svc->>Cache: SET "events::all:Concert:Coldplay" (TTL = 5 min)
        Svc-->>Ctrl: List<EventResponse>
        Ctrl-->>User: HTTP 200 OK (from PostgreSQL)
    else Redis Outage (Fail-Open Fallback)
        Svc->>Cache: Lookup key (Socket timeout = 200ms)
        Cache-->>Svc: RedisConnectionException / Timeout
        Note over Svc: CacheErrorConfig catches error, logs warning,<br/>and treats as cache miss
        Svc->>DB: Fallback query directly to PostgreSQL
        DB-->>Svc: Event records
        Svc-->>Ctrl: List<EventResponse>
        Ctrl-->>User: HTTP 200 OK (Postgres fallback, zero downtime)
    end
```

---

## Detailed Lifecycle 2: Booking Reservation (`POST /api/bookings`)

The booking reservation endpoint is the most critical pipeline in the platform. It enforces both anti-scalping rate limits and concurrency safety.

```mermaid
sequenceDiagram
    autonumber
    actor User as Client (User / Bot)
    participant Sec as JwtAuthFilter
    participant Ctrl as BookingController
    participant Limiter as RateLimiterService (Bucket4j)
    participant Redis as Redis 7
    participant Svc as BookingService
    participant DB as PostgreSQL 16
    participant Ex as GlobalExceptionHandler

    User->>Sec: POST /api/bookings { eventId: 1, seatIds: [10] }<br/>Header: Authorization: Bearer <jwt>
    
    alt Invalid / Missing JWT
        Sec-->>User: HTTP 401 Unauthorized
    else Valid JWT
        Sec->>Ctrl: Forward request with UserDetails (username)
        Ctrl->>Ctrl: Extract Client IP from X-Forwarded-For / RemoteAddr
        Ctrl->>Limiter: checkBookingRateLimit(username, clientIp)

        alt Rate Limiting Check (Redis Healthy)
            Limiter->>Redis: Atomic token consumption (IP & User buckets)
            alt Tokens Exhausted
                Redis-->>Limiter: Tokens = 0 (Refill wait time = X seconds)
                Limiter-->>Ex: Throw RateLimitExceededException(wait)
                Ex-->>User: HTTP 429 Too Many Requests (Retry-After: X)
            else Tokens Available
                Redis-->>Limiter: Token consumed (Remaining = N)
            end
        else Redis Outage (Fail-Open)
            Limiter->>Redis: Connect / CAS command (Timeout 200ms)
            Redis-->>Limiter: Connection timeout
            Note over Limiter: RateLimiterService catches exception,<br/>logs warning, and fails open
        end

        Ctrl->>Svc: createBooking(request, username)
        Svc->>DB: BEGIN Transaction (@Transactional)
        Svc->>DB: SELECT * FROM seats WHERE id IN (10)

        alt Seat Already Held or Booked
            Svc-->>Ex: Throw SeatUnavailableException
            DB->>DB: ROLLBACK Transaction
            Ex-->>User: HTTP 409 Conflict ("Seat is unavailable")
        else Seat Available
            Svc->>DB: UPDATE seats SET status='BOOKED', version=2<br/>WHERE id=10 AND version=1
            
            alt Optimistic Lock Collision (Another thread committed first)
                DB-->>Svc: 0 rows affected
                Svc-->>Ex: Throw ObjectOptimisticLockingFailureException
                DB->>DB: ROLLBACK Transaction (Atomic batch rollback)
                Ex-->>User: HTTP 409 Conflict ("Seat was just booked by another user")
            else Optimistic Lock Success
                DB-->>Svc: 1 row affected
                Svc->>DB: INSERT INTO bookings ...
                Svc->>DB: COMMIT Transaction
                Svc-->>Ctrl: BookingResponse
                Ctrl-->>User: HTTP 201 Created (Booking Confirmed)
            end
        end
    end
```

---

## Detailed Lifecycle 3: Dual-Token Rotation & Reuse Detection

QuickTix protects user sessions using rotating refresh tokens with session family tracking in PostgreSQL:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Browser
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthService
    participant DB as PostgreSQL (refresh_tokens)
    participant Ex as GlobalExceptionHandler

    Note over Client,DB: Stage 1: Login
    Client->>AuthCtrl: POST /api/auth/login { email, password }
    AuthCtrl->>AuthSvc: login(request)
    AuthSvc->>DB: Validate credentials & generate session family UUID
    AuthSvc->>DB: Store SHA-256 hash of refresh token (family_id, used=false)
    AuthSvc-->>Client: Access Token (15m, JSON) + Set-Cookie: refreshToken (7d, HttpOnly)

    Note over Client,DB: Stage 2: Normal Silent Rotation
    Client->>AuthCtrl: POST /api/auth/refresh (Cookie attached automatically)
    AuthCtrl->>AuthSvc: refresh(cookieToken)
    AuthSvc->>DB: Atomic statement: UPDATE refresh_tokens SET used=true WHERE id=? AND used=false
    AuthSvc->>DB: Insert new token hash in same family_id
    AuthSvc-->>Client: New Access Token (JSON) + Rotated HttpOnly Cookie

    Note over Client,DB: Stage 3: Compromise / Replay Attack
    actor Attacker as Attacker (Replaying Stolen Token)
    Attacker->>AuthCtrl: POST /api/auth/refresh (Submits already-used token)
    AuthCtrl->>AuthSvc: refresh(stolenToken)
    AuthSvc->>DB: Find token by SHA-256 hash -> used == true!
    Note over AuthSvc: SECURITY ALERT: Token reuse detected!
    AuthSvc->>DB: Revoke ENTIRE token family (UPDATE refresh_tokens SET revoked=true WHERE family_id=?)
    AuthSvc-->>Ex: Throw BadCredentialsException
    Ex-->>Attacker: HTTP 401 Unauthorized (Cookie wiped with Max-Age=0)
    Note over Client: Both attacker and victim sessions terminated instantly.
```

---

## Detailed Lifecycle 4: Event Modification & Cache Invalidation

When an organizer or administrator modifies event metadata or cancels an event, the system immediately purges distributed cache keys to prevent stale reads:

```
Organizer / Admin: PUT /api/events/{id} or DELETE /api/events/{id}
       │
       ▼
1. Security Filter Chain: Verifies role is ORGANIZER or ADMIN.
2. Ownership Guard: Verifies organizer owns the event (or user is ADMIN).
3. EventService (@Transactional):
   - Updates or deletes record in PostgreSQL.
   - Triggers Spring Cache directive:
     @CacheEvict(value = {"events", "event"}, allEntries = true)
4. Redis 7:
   - Evicts all cached catalog and event detail keys immediately.
5. Response:
   - Returns updated EventResponse (HTTP 200) or No Content (HTTP 204).
6. Next Client Read:
   - Receives fresh data from PostgreSQL and repopulates the Redis cache.
```

---

## Failure Modes & System Resilience Matrix

| Component | Failure Condition | System Behavior | User Impact |
| :--- | :--- | :--- | :--- |
| **Redis 7** | Cache offline / unreachable | `CacheErrorConfig` catches timeout (200ms) and queries PostgreSQL | Catalog queries succeed (HTTP 200), slight latency increase |
| **Redis 7** | Limiter offline / unreachable | `RateLimiterService` fails open and delegates concurrency to Postgres | Bookings succeed (HTTP 201), temporary exposure to bots |
| **PostgreSQL** | Concurrent seat clash | Hibernate `@Version` mismatch throws optimistic locking exception | Competing user receives HTTP 409 Conflict with seat re-selection prompt |
| **Auth JWT** | Access token expired | Axios interceptor pauses queue and calls `/api/auth/refresh` | User experience is uninterrupted; token refreshed silently |
| **Refresh Token**| Token replayed / stolen | System revokes all tokens under `family_id` in PostgreSQL | User and attacker logged out; credentials protected |
