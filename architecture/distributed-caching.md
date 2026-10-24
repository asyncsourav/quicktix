# Distributed Caching Architecture (Redis 7)

**Objective:** Sub-20ms reads on event catalog endpoints (`GET /api/events`) via Redis 7 with automatic cache eviction and fail-open outage resilience.

---

## Cache Policy & Invalidation Strategy

| Cache Name | Cache Key | TTL | Annotation | Trigger / Purpose |
| :--- | :--- | :---: | :--- | :--- |
| **`events`** | `'all:' + #category + ':' + #search` | 5 min | `@Cacheable` | Full event catalog with composite filter keys |
| **`event`** | `#id` | 5 min | `@Cacheable` | Single event detail view by ID |
| **Eviction** | All entries in `events`, `event` | Immediate | `@CacheEvict(allEntries=true)` | Fired on event create, update, or cancel in `EventService.java` |

* **TTL Safety Net:** In [`RedisConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/RedisConfig.java), 5-minute TTL (`Duration.ofMinutes(5)`) and `disableCachingNullValues()` prevent stale data leaks and memory bloat.
* **Eviction Verification:** Verified via [`scripts/check-eviction.mjs`](../scripts/check-eviction.mjs) (updates event and proves immediate cache refresh on next read).

---

## Fail-Open Outage Resilience (`CacheErrorConfig`)

Default Spring Cache throws `RedisConnectionException` on Redis downtime, returning HTTP 500. QuickTix overrides this behavior in [`CacheErrorConfig.java`](../server/src/main/java/com/asyncsourav/quicktix/config/CacheErrorConfig.java) using `CacheErrorHandler`:

```java
@Configuration
public class CacheErrorConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache c, Object k) {
                log.warn("Cache get failed for '{}', falling back to DB: {}", c != null ? c.getName() : "unknown", e.toString());
            }
            @Override
            public void handleCachePutError(RuntimeException e, Cache c, Object k, Object v) {
                log.warn("Cache put failed: {}", e.toString());
            }
            @Override
            public void handleCacheEvictError(RuntimeException e, Cache c, Object k) {
                log.warn("Cache evict failed: {}", e.toString());
            }
            @Override
            public void handleCacheClearError(RuntimeException e, Cache c) {
                log.warn("Cache clear failed: {}", e.toString());
            }
        };
    }
}
```

* **Result:** Redis failure or timeout is treated as a **cache miss**. Requests fall back to PostgreSQL and return **HTTP 200 OK** with zero attendee downtime.

---

## Benchmark Verification ([`scripts/latency.mjs`](../scripts/latency.mjs))

Measured across 200 sequential requests per endpoint with 1,000 seeded events in PostgreSQL:

| Endpoint | Metric | PostgreSQL Direct | Redis 7 Cached | Latency Reduction |
| :--- | :--- | :---: | :---: | :---: |
| **`GET /api/events`** (1,000 items) | Average Latency | 24.7 ms | **18.4 ms** | **25.5% faster** |
| | p50 Median | 22.1 ms | **17.7 ms** | **19.9% faster** |
| | p95 Latency | 38.9 ms | **31.2 ms** | **19.8% faster** |
| **`GET /api/events/1`** (Detail view) | Average Latency | 15.2 ms | **13.7 ms** | **10.5% faster** |
| | p50 Median | 15.3 ms | **13.7 ms** | **10.5% faster** |
| | p95 Latency | 21.8 ms | **20.4 ms** | **6.4% faster** |
