package com.yu.framework.cache;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 基于 Redis 的分布式锁（无额外第三方依赖，复用工程内已配置的 RedisTemplate）。
 *
 * <p>用于多实例部署下保护竞态敏感的写路径（如选课抽签、排课落库、状态数据报盘生成），
 * 弥补此前仅依赖 Redis DECR 的“单实例假设”。加锁使用 {@code SET key token NX PX lease}
 * 语义（{@link org.springframework.data.redis.connection.RedisStringCommands.SetOption}），
 * 解锁通过 Lua 脚本比对持有者 token 后删除，避免误删他人锁。</p>
 *
 * <p>注意：Redis 分布式锁为“尽力而为”的互斥，非严格可重入；如需看门狗自动续期或可重入，
 * 应接入 Redisson 等专用实现。当前租约时间应显著大于被保护业务的执行时长。</p>
 *
 * @author cjlu
 */
@Component
public class DistributedLock
{
    /** 锁键统一前缀，隔离业务键空间 */
    private static final String LOCK_PREFIX = "lock:";

    /** 释放锁脚本：仅当持有者 token 匹配时才删除，保证解锁安全 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static
    {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
    }

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 尝试获取锁（自旋重试至等待超时）。
     *
     * @param key         业务锁键（内部自动加 lock: 前缀）
     * @param waitTimeMs  最长等待时间（毫秒），<=0 表示只尝试一次
     * @param leaseTimeMs 锁租约（毫秒），到期自动释放以防死锁
     * @return 获取成功返回持有者 token（解锁时需回传），失败返回 {@code null}
     */
    public String tryLock(String key, long waitTimeMs, long leaseTimeMs)
    {
        String lockKey = LOCK_PREFIX + key;
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + Math.max(0, waitTimeMs);
        do
        {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, leaseTimeMs, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(ok))
            {
                return token;
            }
            if (System.currentTimeMillis() >= deadline)
            {
                return null;
            }
            try
            {
                // 短暂退避，避免高频轮询打满 Redis
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        while (System.currentTimeMillis() <= deadline);
        return null;
    }

    /**
     * 释放锁（token 匹配才删除）。
     *
     * @param key   业务锁键
     * @param token {@link #tryLock} 返回的持有者 token
     * @return 是否成功释放
     */
    public boolean unlock(String key, String token)
    {
        if (token == null)
        {
            return false;
        }
        String lockKey = LOCK_PREFIX + key;
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        return result != null && result > 0;
    }
}
