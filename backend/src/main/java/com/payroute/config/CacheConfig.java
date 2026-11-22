package com.payroute.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig {

    public static final String CACHE_PROVIDERS = "providers";
    public static final String CACHE_ROUTING_RULES = "routing_rules";
    public static final String CACHE_MERCHANTS = "merchants";

    @Value("${payroute.cache.ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Bean
    @Primary
    public CacheManager cacheManager(@org.springframework.beans.factory.annotation.Autowired(required = false) RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory != null) {
            try {
                // Test ping connection
                redisConnectionFactory.getConnection().ping();
                log.info("Redis is available. Configuring RedisCacheManager with TTL: {}s", cacheTtlSeconds);

                RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(cacheTtlSeconds))
                        .disableCachingNullValues()
                        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

                return RedisCacheManager.builder(redisConnectionFactory)
                        .cacheDefaults(config)
                        .build();
            } catch (Exception e) {
                log.warn("Redis ping failed ({}), falling back to in-memory ConcurrentMapCacheManager", e.getMessage());
            }
        }

        log.info("Using in-memory ConcurrentMapCacheManager");
        return new ConcurrentMapCacheManager(CACHE_PROVIDERS, CACHE_ROUTING_RULES, CACHE_MERCHANTS);
    }
}
