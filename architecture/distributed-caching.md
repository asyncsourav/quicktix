# Distributed Caching Architecture (Redis 7)

This document describes the caching architecture, TTL invalidation strategy, and fail-open resilience patterns implemented with Redis 7 and Spring Cache.

---

## Architecture Overview

Event browsing accounts for more than 90% of total application traffic. To protect PostgreSQL from repetitive read queries during traffic surges, QuickTix implements distributed in-memory caching via Redis 7:

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

## Cache Configuration & Invalidation Strategy

### 1. Read Operations (`@Cacheable`)
* **Event Catalog (`GET /api/events`)**:
  ```java
  @Cacheable(value = "events", key = "'all:' + #category + ':' + #search")
  public List<EventResponse> getAllEvents(String category, String search) { ... }
  ```
  Composite keys segment queries by category filter and search keyword.
* **Event Detail (`GET /api/events/{id}`)**:
  ```java
  @Cacheable(value = "event", key = "#id")
  public EventResponse getEventById(Long id) { ... }
  ```

### 2. Write Operations & Cache Invalidation (`@CacheEvict`)
To prevent stale reads, creating, modifying, or deleting an event immediately evicts the cached entries (verified via [`scripts/check-eviction.mjs`](../scripts/check-eviction.mjs)):
```java
@CacheEvict(value = {"events", "event"}, allEntries = true)
public EventResponse updateEvent(Long id, EventRequest request, String username) { ... }
```

### 3. Background Expiration (TTL Safety Net)
Configured in [`RedisConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/RedisConfig.java):
* Default TTL: **5 Minutes** (`Duration.ofMinutes(5)`).
* Prevents inactive event entries from consuming Redis memory indefinitely.
* Null values are explicitly omitted (`disableCachingNullValues()`).

---

## Outage Resilience: Fail-Open Error Handling

A distributed cache is an optimization layer, not a hard dependency for correctness. An outage in Redis must never bring down the event ticketing service.

### [`CacheErrorConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java)
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
            ...
        };
    }
}
```

* **Default Spring Behavior**: If Redis is unreachable, Spring throws a `QueryTimeoutException` or `RedisConnectionException` and returns **HTTP 500**.
* **QuickTix Behavior**: `CacheErrorConfig` logs a warning and suppresses the exception. Spring treats the failure as a **cache miss** and falls back directly to PostgreSQL, returning **HTTP 200**.

---

## Latency Benchmark (1,000 Seeded Events)
 
Timing 200 sequential requests per endpoint via [`scripts/latency.mjs`](../scripts/latency.mjs) against the live containerized stack:

| Endpoint | Metric | Run A: PostgreSQL Only | Run B: Redis 7 Cache | Improvement |
| :--- | :--- | :--- | :--- | :--- |
| **`GET /api/events`** (1,000 items) | Average Latency | **24.7 ms** | **18.4 ms** | **25.5% faster** |
| | p50 Median | **22.1 ms** | **17.7 ms** | **19.9% faster** |
| | p95 Latency | **38.9 ms** | **31.2 ms** | **19.8% faster** |
| **`GET /api/events/1`** (Detail view) | Average Latency | **15.2 ms** | **13.7 ms** | **10.5% faster** |
| | p50 Median | **15.3 ms** | **13.7 ms** | **10.5% faster** |
| | p95 Latency | **21.8 ms** | **20.4 ms** | **6.4% faster** |
