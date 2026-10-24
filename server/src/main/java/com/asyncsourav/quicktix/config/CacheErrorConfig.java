


package com.asyncsourav.quicktix.config;




import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;





/**
 * Redis outage must never take the application down
 * CacheErrorConfig intercepts cache errors. 
 * Instead of crashing, it logs a warning and quietly lets the method continue. 
 * Spring treats this as a cache miss and falls back directly to PostgreSQL, returning HTTP 200 OK.
 */



/**
 * Because your handleCacheGetError simply logs a warning and does not re-throw the exception, 
 * Spring assumes it was a normal cache miss, falls through to PostgreSQL, and returns 200 OK
 */
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
