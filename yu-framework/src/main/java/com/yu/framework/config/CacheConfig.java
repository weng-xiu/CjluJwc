package com.yu.framework.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * A3：Spring Cache 抽象 + Redis 落地配置。
 *
 * <p>本工程此前仅开启 {@code @EnableCaching} 而无任何 {@code CacheManager} 定制，
 * Spring Boot 默认装配的 RedisCacheManager 不带 TTL（命中即永久缓存），存在脏数据长期滞留风险。
 * 这里显式提供带「按缓存名 TTL」的 RedisCacheManager，值序列化复用工程既有
 * {@link FastJson2JsonRedisSerializer}（WriteClassName + com.yu 白名单，可安全还原领域对象类型），
 * 键使用 StringRedisSerializer 保证可读性与既有 redisTemplate 键风格一致。</p>
 *
 * <p>注意：Spring Boot 4.x 已移除 {@code CachingConfigurerSupport}，故不再继承该类，
 * 直接暴露 {@code CacheManager} Bean 即可让自动装配退避。</p>
 *
 * @author cjlu
 */
@Configuration
public class CacheConfig
{
    /** 缓存键统一前缀，隔离 Spring Cache 与业务直连 Redis 键，避免误清理 */
    private static final String CACHE_KEY_PREFIX = "cache:";

    /** current_semester：当前学期，全年仅切换约两次，读多写极少，TTL 10 分钟 */
    public static final String CACHE_CURRENT_SEMESTER = "current_semester";

    /** student_schedule：学生课表，门户高频读；受选课/排课/开课三方影响，用短 TTL 120s 自愈 */
    public static final String CACHE_STUDENT_SCHEDULE = "student_schedule";

    /** 默认 TTL：未显式登记的缓存名用此值，防止无界缓存 */
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory)
    {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .prefixCacheNameWith(CACHE_KEY_PREFIX)
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new FastJson2JsonRedisSerializer<Object>(Object.class)))
                // 不缓存 null：避免降级/空结果长期占据，下次请求可正常回源
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CACHE_CURRENT_SEMESTER, base.entryTtl(Duration.ofMinutes(10)));
        perCache.put(CACHE_STUDENT_SCHEDULE, base.entryTtl(Duration.ofSeconds(120)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }
}
