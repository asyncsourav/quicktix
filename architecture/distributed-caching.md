# Distributed Caching Architecture (Redis 7)

This document details how I designed, configured, and benchmarked the distributed caching layer in QuickTix using Redis 7 and Spring Cache, including the cache failure handling I built into `CacheErrorConfig`.

---

## Why I Built This: The Read Bottleneck

When analyzing event ticketing traffic patterns, I observed that catalog browsing accounts for more than 90% of all requests. During a live drop, thousands of concurrent attendees repeatedly refresh the homepage, search for performers, and view event descriptions.

Hitting PostgreSQL for every single read query would:
1. Waste database connection pool threads on static data.
2. Spike database CPU and memory bandwidth with repetitive table joins.
3. Slow down catalog response times.

To solve this, I introduced an in-memory distributed caching layer via Redis 7:

```
[ Incoming Read Request ]
          │
          ▼
[ EventService.getAllEvents / getEventById ]
          │
          ▼
   Is data in Redis?
    ├── YES (Cache Hit): Return serialized JSON immediately (< 20ms)
    └── NO  (Cache Miss): Query PostgreSQL, store in Redis with 5m TTL, return data
```

---

## How I Configured Caching & Invalidation

### 1. Read Operations (`@Cacheable`)
In [`EventService.java`](../server/src/main/java/com/asyncsourav/quicktix/service/EventService.java), I applied `@Cacheable` to my hottest read paths:
* **Event Catalog (`GET /api/events`)**:
  ```java
  @Cacheable(value = "events", key = "'all:' + #category + ':' + #search")
  public List<EventResponse> getAllEvents(String category, String search) { ... }
  ```
  I constructed composite keys using the category filter and search keyword so queries with different filters cache into independent entries.
* **Event Detail (`GET /api/events/{id}`)**:
  ```java
  @Cacheable(value = "event", key = "#id")
  public EventResponse getEventById(Long id) { ... }
  ```

### 2. Solving Stale Reads with Invalidation (`@CacheEvict`)
The biggest flaw in basic caching architectures is serving stale data. If an organizer updates an event's start time or price, users must not see yesterday's information.

Whenever an event is created, modified, or cancelled, I trigger an immediate cache flush:
```java
@CacheEvict(value = {"events", "event"}, allEntries = true)
public EventResponse updateEvent(Long id, EventRequest request, String username) { ... }
```
I verified this behavior using my automated test script [`scripts/check-eviction.mjs`](../scripts/check-eviction.mjs), which updates an event and proves that the fresh data is returned on the very next read.

### 3. Background TTL Safety Net
In [`RedisConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/RedisConfig.java), I set a 5-minute time-to-live (`Duration.ofMinutes(5)`) with `disableCachingNullValues()`. Even if an unforeseen edge case bypasses eviction, inactive keys expire automatically and prevent unbounded Redis memory growth.

---

## The Outage Bug I Discovered & How I Fixed It (`CacheErrorConfig`)

While testing disaster recovery (stopping the Redis container with `docker compose stop redis`), I encountered a critical flaw in Spring's default cache implementation:

* **The Problem:** By default, if Redis is down or unreachable, Spring Cache throws a `RedisConnectionException` or `QueryTimeoutException`. This unhandled runtime exception bubbles up through the controller and returns an **HTTP 500 Internal Server Error** to the user.
* **Why This Was Unacceptable:** A cache is an auxiliary optimization layer. An outage in Redis should never take down the core event catalog.
* **How I Fixed It:** I created [`CacheErrorConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java) implementing `CachingConfigurer.errorHandler()`:

```java
@Slf4j
@Configuration
public class CacheErrorConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache c, Object k) {
                log.warn("Cache get failed for cache '{}', key '{}': {}", c != null ? c.getName() : "unknown", k, e.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) {
                log.warn("Cache put failed for cache '{}', key '{}': {}", c != null ? c.getName() : "unknown", k, e.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache c, Object k) {
                log.warn("Cache evict failed for cache '{}', key '{}': {}", c != null ? c.getName() : "unknown", k, e.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache c) {
                log.warn("Cache clear failed for cache '{}': {}", c != null ? c.getName() : "unknown", e.toString());
            }
        };
    }
}
```

By intercepting and logging the cache exception, Spring treats the failure as a **cache miss**. The request seamlessly falls back directly to PostgreSQL and returns **HTTP 200 OK** with zero downtime for attendees.

---

## What I Measured: Latency Benchmark (1,000 Seeded Events)

To measure the performance difference with concrete numbers, I wrote [`scripts/latency.mjs`](../scripts/latency.mjs) and ran 200 sequential requests per endpoint against the live containerized stack:

| Endpoint | Metric | Run A: PostgreSQL Only | Run B: Redis 7 Cache | Improvement |
| :--- | :--- | :--- | :--- | :--- |
| **`GET /api/events`** (1,000 items) | Average Latency | **24.7 ms** | **18.4 ms** | **25.5% faster** |
| | p50 Median | **22.1 ms** | **17.7 ms** | **19.9% faster** |
| | p95 Latency | **38.9 ms** | **31.2 ms** | **19.8% faster** |
| **`GET /api/events/1`** (Detail view) | Average Latency | **15.2 ms** | **13.7 ms** | **10.5% faster** |
| | p50 Median | **15.3 ms** | **13.7 ms** | **10.5% faster** |
| | p95 Latency | **21.8 ms** | **20.4 ms** | **6.4% faster** |
