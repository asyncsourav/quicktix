# Distributed Rate Limiting & Anti-Scalping Architecture

This document covers the distributed rate limiting implementation on the booking endpoint (`POST /api/bookings`) using Bucket4j, Lettuce, and Redis 7.

---

## The Threat Model: Ticket-Scalping Bots

During high-demand ticket drops, automated scalper bots use scripted parallel connections to send hundreds of booking requests per second. While database optimistic locking (`@Version`) guarantees that duplicate bookings are rejected, thousands of concurrent failing transactions will:
1. Exhaust PostgreSQL's HikariCP connection pool.
2. Degrade database I/O with continuous transaction rollbacks.
3. Starve legitimate human attendees trying to check out.

Rate limiting acts as an **edge filter at the controller boundary**, rejecting abusive request volume before it initiates database transactions.

---

## Token Bucket Algorithm (Bucket4j) vs Fixed Window

* **Fixed Window (Flawed)**: Counting requests in static 1-minute blocks creates a "window edge" vulnerability where an attacker sends 5 requests at `00:59` and 5 more at `01:00`, doubling the intended throughput.
* **Token Bucket (Bucket4j)**: A virtual bucket accumulates tokens continuously at a refill rate up to a maximum burst capacity. This accommodates legitimate bursts (e.g. rapid retries) while throttling continuous automated spam.

---

## Dual-Tier Limit Policy

QuickTix enforces two independent bucket limits on every booking attempt:

```
POST /api/bookings
       │
       ├── 1. Check IP Bucket: "rate:booking:ip:{clientIp}"
       │      Capacity: 10 tokens | Refill: 2 tokens/sec
       │      Purpose: Blocks bot farms hammering from a single IP/proxy
       │
       └── 2. Check User Bucket: "rate:booking:user:{username}"
              Capacity: 5 tokens  | Refill: 1 token/sec
              Purpose: Blocks authenticated single-account automation scripts
```

---

## Redis Synchronization via Lettuce (CAS)

Local in-memory rate limiters fail when scaling horizontally across multiple backend instances. QuickTix delegates token state tracking to Redis via the Lettuce driver:
* **Compare-And-Swap (CAS)**: Bucket4j uses atomic CAS loops to increment/decrement token counts inside Redis across all backend pods.
* **Shared State**: All application instances read from and write to the same distributed keys, preventing attackers from bypassing limits by spraying requests across different pods.

---

## Outage Resilience: Fail-Open Strategy

Rate limiting protects system throughput, but **PostgreSQL optimistic locking guarantees booking correctness**. If Redis suffers an outage or network partition, rejecting all bookings would cause a total revenue stoppage for the business.

Therefore, the rate limiter **fails open**:
```java
ConsumptionProbe probe;
try {
    ProxyManager<String> pm = getProxyManager();
    if (pm == null) {
        log.warn("Rate limiter proxy manager unavailable, failing open for key: {}", key);
        return true;
    }
    Bucket bucket = pm.builder().build(key, () -> config);
    probe = bucket.tryConsumeAndReturnRemaining(1);
} catch (Exception e) {
    log.warn("Rate limiter unavailable, failing open: {}", e.toString());
    return true; // Let request through to PostgreSQL
}

if (!probe.isConsumed()) {
    long wait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    throw new RateLimitExceededException(wait);
}
return true;
```

* **Short Timeouts (200ms)**: Configured in [`RateLimiterConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/RateLimiterConfig.java) to ensure a down Redis instance does not block user threads for more than 200ms before failing open.

---

## HTTP 429 Error Envelope

When limits are exceeded, [`GlobalExceptionHandler.java`](../server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java) returns standard headers and a structured JSON body:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 1
X-Rate-Limit-Retry-After-Seconds: 1
Content-Type: application/json

{
  "timestamp": "2026-10-24T08:15:00.124",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Too many booking requests. Please wait 1 second before trying again.",
  "path": "/api/bookings"
}
```
