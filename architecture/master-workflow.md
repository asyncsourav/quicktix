# Master Request Lifecycle & System Pipeline

**Objective:** Trace how requests traverse security, validation, distributed rate limiting, caching, and optimistic locking to final HTTP dispatch.

---

## High-Level Request Pipeline

```
[ Browser / Frontend Client ]
             │
             │ HTTPS (In-Memory Bearer Token + HttpOnly Cookie)
             ▼
[ 1. Spring Security Filter Chain ]
     ├── CorsFilter (Dynamic YAML origin verification via CorsProperties)
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
[ 4. Service Layer ]
     ├── Business Logic Orchestration (@Transactional boundaries)
     └── Ownership & Role Verification (User, Organizer, Admin)
             │
             ▼
[ 5. Distributed Caching Layer (Redis 7) ]
     ├── Cache Lookup (@Cacheable hot paths in Redis)
     │     ├── Cache Hit: Return cached JSON immediately (< 20ms)
     │     ├── Cache Miss: Proceed to database query
     │     └── Cache Error: Fail-open fallback via CacheErrorConfig
     └── Cache Eviction (@CacheEvict purges stale catalog keys on updates)
             │
             ▼
[ 6. Persistence & Concurrency Control (PostgreSQL 16) ]
     ├── Optimistic Locking: Hibernate @Version check on Seat (WHERE id=? AND version=?)
     ├── ACID Commit: Commit booking or trigger atomic batch rollback on collision
     ├── Real-Time Broadcast: SimpMessagingTemplate pushes status to /topic/event.{id}
     └── Async Notification: @Async EmailService dispatches receipt on mailTaskExecutor
             │
             ▼
[ 7. Response Dispatch ]
     ├── HTTP 200 / 201: ApiResponse<T> JSON envelope (unblocked by email dispatch)
     ├── HTTP 409: Seat conflict (ObjectOptimisticLockingFailureException)
     └── HTTP 429: Rate limit exceeded (Retry-After header attached)
```

---

## Workflow 1: Live Event Catalog Read Flow (`GET /api/events`)

```mermaid
flowchart TD
    Client([Attendee Browser]) --> Web[EventController]
    Web --> Svc[EventService]
    Svc --> Cache{Is Data in Redis?}
    
    Cache -->|Cache Hit| FastRet([HTTP 200 ~18ms - Instant Return])
    Cache -->|Cache Miss / Outage| DB[(PostgreSQL 16)]
    DB --> CacheSave[Save to Redis - TTL: 5 min]
    CacheSave --> Ret([HTTP 200 OK - Database Fallback])
```

* **Cache Key:** `events::all:{category}:{search}`. Hit returns in ~18ms.
* **Fail-Open:** If Redis is down, `CacheErrorConfig` falls back to PostgreSQL (HTTP 200 OK).
* **Eviction:** Mutations trigger `@CacheEvict(value = {"events", "event"}, allEntries = true)`.
* **Deep Dive:** [`distributed-caching.md`](distributed-caching.md).

---

## Workflow 2: Ticket Booking & Concurrency Flow (`POST /api/bookings`)

```mermaid
flowchart TD
    Client([Attendee / Bot]) --> Sec[JwtAuthFilter]
    Sec --> Ctrl[BookingController]
    Ctrl --> Limiter{Bucket4j Rate Limiter}
    
    Limiter -->|Rate Limit Exceeded| Throttled([HTTP 429 Too Many Requests])
    Limiter -->|Tokens Available / Fail-Open| Svc[BookingService]
    
    Svc --> DB{PostgreSQL @Version Check}
    DB -->|Version Match - Winner| Commit[Commit ACID Transaction]
    Commit --> STOMP[SimpMessagingTemplate: Broadcast /topic/event.id]
    Commit --> Email["@Async EmailService: Background dispatch on mailTaskExecutor"]
    Commit --> Confirmed([HTTP 201 Booking Confirmed])
    DB -->|Version Conflict - Loser| Conflict([HTTP 409 Seat Conflict - Rollback])
```

* **Anti-Scalping:** Bucket4j IP bucket (burst 10) & User bucket (burst 5). Exceeded capacity returns HTTP 429.
* **Fail-Open:** Redis outage or 200ms timeout bypasses limiter to PostgreSQL.
* **Optimistic Locking:** `UPDATE seats SET status='BOOKED', version=2 WHERE id=:id AND version=1`.
  * Winner (1 row affected): HTTP 201 Created.
  * Loser (0 rows affected): OptimisticLockingFailure -> transaction rollback -> HTTP 409 Conflict.
* **Async Decoupling:** `EmailService` dispatches confirmation emails on a background thread pool (`mailTaskExecutor`). Zero impact on HTTP response latency even if SMTP is slow or unreachable.
* **Deep Dives:** [`distributed-rate-limiting.md`](distributed-rate-limiting.md) and [`concurrency-control.md`](concurrency-control.md).

---

## Workflow 3: Dual-Token Authentication & Rotation Flow (`/api/auth/*`)

### Phase 1: Login & Token Issuance
```mermaid
flowchart TD
    LoginReq([POST /api/auth/login]) --> AuthCtrl[AuthController]
    AuthCtrl --> GenTokens[Generate Access Token + Refresh Token]
    GenTokens --> SaveHash[(Save SHA-256 Hash + family_id in PostgreSQL)]
    SaveHash --> AuthResp([HTTP 200 OK: In-Memory Access Token + HttpOnly Cookie])
```

### Phase 2: Silent Rotation & Replay Defense
```mermaid
flowchart TD
    RefReq([POST /api/auth/refresh]) --> CheckClaim{Atomic Database Claim}
    CheckClaim -->|used == false - Normal Rotation| Rotate[Burn Token + Issue Rotated Token Pair]
    Rotate --> RefResp([HTTP 200 OK: New Access Token + Rotated HttpOnly Cookie])
    CheckClaim -->|used == true - Replay Attack| Trap[SECURITY ALERT: Token Reuse Detected!]
    Trap --> Revoke[(Revoke Entire Token Family in PostgreSQL)]
    Revoke --> Wipe([HTTP 401 Unauthorized + Cookie Wiped])
```

* **Issuance:** 15-minute access token in memory + 7-day refresh token in HttpOnly cookie.
* **Rotation:** Atomic DB claim prevents simultaneous refresh race conditions.
* **Replay Trap:** Reuse detection revokes entire session family in PostgreSQL via `@Transactional(noRollbackFor = BadCredentialsException.class)`.
* **Deep Dive:** [`security-and-auth.md`](security-and-auth.md).
