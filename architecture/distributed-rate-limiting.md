# Distributed Rate Limiting & Anti-Scalping Architecture

**Objective:** Throttle ticket-scalping bots on `POST /api/bookings` before database transactions execute.

---

## Rate Limiting Policy (Bucket4j + Redis 7)

| Bucket Tier | Distributed Redis Key | Burst Capacity | Refill Rate | Protection Scope |
| :--- | :--- | :---: | :---: | :--- |
| **IP Tier** | `rate:booking:ip:{clientIp}` | 10 tokens | 2 tokens/sec | Blocks unauthenticated bot proxies & IP clusters |
| **User Tier** | `rate:booking:user:{username}` | 5 tokens | 1 token/sec | Blocks authenticated single-account automation scripts |

* **Client IP Extraction:** Extracted in [`BookingController.java`](../server/src/main/java/com/asyncsourav/quicktix/controller/BookingController.java) via `X-Forwarded-For` and `X-Real-IP` with `getRemoteAddr()` fallback.
* **State Synchronization:** Distributed token state synchronized across application pods using atomic Compare-And-Swap (CAS) via Lettuce Redis client.

---

## Fail-Open Strategy (Redis Outage Resilience)

PostgreSQL optimistic locking guarantees booking correctness. If Redis fails or times out (200ms socket timeout), requests fail open to preserve sales:

```java
try {
    Bucket bucket = proxyManager.builder().build(key, () -> config);
    return bucket.tryConsumeAndReturnRemaining(1).isConsumed();
} catch (Exception e) {
    log.warn("Rate limiter down, failing open: {}", e.toString());
    return true; // Fall through to PostgreSQL
}
```

---

## HTTP 429 Rejection Envelope

Standard headers (`Retry-After: 1`, `X-Rate-Limit-Retry-After-Seconds: 1`) with uniform JSON body:

```json
{
  "timestamp": "2026-10-24T08:15:00.124",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many booking requests. Please wait 1 second before trying again.",
  "path": "/api/bookings"
}
```

---

## Benchmark Verification ([`scripts/rate-limit.mjs`](../scripts/rate-limit.mjs))

* **Phase 1 (1,000 rapid-fire requests, 100 concurrent):**
  * Time: 1.90 seconds.
  * Verdict: 6 allowed (5 burst + 1 refill token), **994 blocked with HTTP 429 (99.4% blocked)**.
  * Database: 0 connection pool errors.
* **Phase 2 (Sustained 4 req/sec for 10 seconds):**
  * Allowed: 11 | Blocked: 29.
  * Measured Rate: **1.1 allowed/sec** (matches configured 1 token/sec refill rate).
