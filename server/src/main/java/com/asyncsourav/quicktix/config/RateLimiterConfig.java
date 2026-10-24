

package com.asyncsourav.quicktix.config;



import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;



@Configuration
public class RateLimiterConfig {


    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

   
    // With 200ms timeouts, the rate limiter detects the issue almost instantly, logs the warning, and fails open (allow the request to proceed)
    @Value("${spring.data.redis.timeout:200ms}")
    private Duration redisTimeout;


    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient() {

        RedisURI redisUri = RedisURI.builder()
                .withHost(redisHost)
                .withPort(redisPort)
                .withTimeout(redisTimeout)
                .build();

        RedisClient client = RedisClient.create(redisUri);
        client.setOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(redisTimeout)
                        .build())
                .autoReconnect(true)
                .build());
        return client;
    }
}
