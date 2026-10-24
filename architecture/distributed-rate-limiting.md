# Distributed Rate Limiting & Anti-Scalping Architecture

This document details how I designed, implemented, and verified distributed rate limiting on the booking endpoint (`POST /api/bookings`) using Bucket4j, Lettuce, and Redis 7.

---

## Why I Built This: The Scalper Bot Threat

While building QuickTix, I implemented Hibernate `@Version` optimistic locking to guarantee that two users can never double-book the same seat. But as I thought through high-concurrency ticket drop scenarios, I realized a serious bottleneck:

Automated ticket-scalping bots use scripted parallel connections to hammer the booking endpoint with hundreds of requests per second. Even though my database correctly rejects duplicate attempts with HTTP 409, thousands of failing transactions will:
1. Exhaust PostgreSQL's HikariCP connection pool threads.
2. Degrade database I/O through continuous transaction rollbacks.
3. Starve legitimate human attendees trying to check out.

To solve this, I designed an edge rate limiter directly at the controller boundary in [`BookingController.java`](../server/src/main/java/com/asyncsourav/quicktix/controller/BookingController.java) to intercept and throttle bot spam before it ever initiates a database transaction.

---

## Why I Chose Token Bucket (Bucket4j) Over Fixed Window

When evaluating rate-limiting algorithms, I compared Fixed Window counters against Token Buckets:
* **Why I Rejected Fixed Window:** Fixed-window counting (e.g., max 5 requests per calendar minute) suffers from the "window edge" vulnerability. An attacker can fire 5 requests at `00:59` and 5 more at `01:00`, effectively sending 10 requests in 2 seconds and doubling the intended traffic threshold.
* **Why I Implemented Token Bucket:** A virtual bucket accumulates tokens continuously at a refill rate up to a max burst capacity. This accommodates realistic human behavior (such as an attendee rapidly clicking confirm or retrying once) while mathematically capping sustained automated spam.

---

## My Dual-Tier Limiting Policy

I decided to enforce two independent token bucket limits on every `POST /api/bookings` call:

```
POST /api/bookings
       │
       ├── 1. IP Tier: "rate:booking:ip:{clientIp}"
       │      Capacity: 10 tokens | Refill: 2 tokens/sec
       │      Why I added this: Stops bot farms hammering from a single IP/proxy before authentication
       │
       └── 2. User Tier: "rate:booking:user:{username}"
              Capacity: 5 tokens  | Refill: 1 token/sec
              Why I added this: Blocks authenticated users running automated booking scripts
```

In [`BookingController.java`](../server/src/main/java/com/asyncsourav/quicktix/controller/BookingController.java), I extracted the client IP by inspecting `X-Forwarded-For` and `X-Real-IP` reverse proxy headers with fallback to `request.getRemoteAddr()`, ensuring the IP limit works accurately behind Docker or load balancers.

---

## Distributed Synchronization with Redis & Lettuce

In-memory rate limiters (like Guava or simple local maps) fail when horizontal scaling adds multiple backend instances, because an attacker can spray requests across different pods.

To solve this, I configured Bucket4j to synchronize distributed token state in Redis 7 using the Lettuce driver in [`RateLimiterConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/RateLimiterConfig.java):
* **Atomic Compare-And-Swap (CAS):** Bucket4j executes atomic CAS state updates in Redis across all backend pods.
* **Shared Cluster State:** Every backend pod checks the exact same distributed keys, making it impossible for bots to bypass limits by rotating connections across instances.

---

## The Outage Edge Case: Why I Implemented Fail-Open Resilience

While designing the rate limiter, I asked myself a critical failure-mode question: *What happens if Redis crashes or suffers a network partition?*

If the rate limiter threw an unhandled exception or rejected all requests when Redis was down, legitimate attendees would be blocked from purchasing tickets, causing total business revenue stoppage. 

Because **PostgreSQL optimistic locking already guarantees 100% data correctness**, rate limiting is an optimization layer, not a safety dependency. Therefore, I implemented a strict **fail-open** design in [`RateLimiterService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/RateLimiterService.java):

```java
ConsumptionProbe probe;
try {
    ProxyManager<String> pm = getProxyManager();
    if (pm == null) {
        log.warn("Rate limiter proxy manager unavailable, failing open for key: {}", key);
        return true; // Let request proceed to database
    }
    Bucket bucket = pm.builder().build(key, () -> config);
    probe = bucket.tryConsumeAndReturnRemaining(1);
} catch (Exception e) {
    log.warn("Rate limiter unavailable, failing open: {}", e.toString());
    return true; // Let request proceed to database
}

if (!probe.isConsumed()) {
    long wait = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    throw new RateLimitExceededException(wait);
}
return true;
```

* **Short Timeouts (200ms):** I configured standalone Lettuce connection and command timeouts to `200ms` in `RateLimiterConfig.java`. If Redis is down, user requests are delayed by at most 200ms before failing open directly to PostgreSQL, avoiding thread starvation.

---

## HTTP 429 Error Envelope & Rejection Experience

When a client exceeds the token bucket limit, I throw a custom `RateLimitExceededException` that passes the required refill wait seconds. 

In [`GlobalExceptionHandler.java`](../server/src/main/java/com/asyncsourav/quicktix/exception/GlobalExceptionHandler.java), I catch this exception and attach standard HTTP rate-limiting headers alongside my uniform error JSON:

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
