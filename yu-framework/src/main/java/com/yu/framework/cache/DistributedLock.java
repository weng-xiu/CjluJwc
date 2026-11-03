package com.yu.framework.cache;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * 基于 Redis 的分布式锁（无额外第三方依赖，直接用 {@link RedisConnectionFactory} 的字节命令）。
 *
 * <p>用于多实例部署下保护竞态敏感的写路径（如选课抽签、排课落库、状态数据报盘生成），
 * 弥补此前仅依赖 Redis DECR 的“单实例假设”。加锁使用 {@code SET key token NX PX lease}
 * 语义（{@link SetOption}），解锁通过 Lua 脚本比对持有者 token 后删除，避免误删他人锁。</p>
 *
 * <p><b>T-5：为什么绕开 RedisTemplate 的序列化器。</b>早先版本用
 * {@code RedisTemplate<Object, Object>} 的 {@code setIfAbsent} / {@code execute(script, ...)}，
 * 锁值与 Lua 的 ARGV 都隐式经过 valueSerializer（工程内是 FastJson2，String 会被写成带双引号的
 * JSON 字面量）。锁值、ARGV、脚本内 {@code redis.call('get')} 三者只要有一处序列化策略不同
 * （换 Jackson、换 JDK 序列化、某个 template 被单独配置），比较就会静默失配——表现为“锁永远
 * 解不掉，只能等 TTL 过期”，是典型的隐式耦合。现在全链路显式使用 UTF-8 原始字节，
 * 不再依赖任何 valueSerializer。</p>
 *
 * <p><b>T-5：为什么需要看门狗续期。</b>固定租约只要短于任务时长，锁就会在任务跑到一半时过期，
 * 另一个实例随即拿到锁并发执行同一份全量重算。{@link #tryAcquire} 返回的句柄会在租约的 1/3
 * 处自动 {@code PEXPIRE} 续期，直到任务结束 {@link LockHandle#close()}；续期本身也做 token 校验，
 * 锁真的丢失（实例重启、TTL 已过、被他人持有）时立即停止续期并留痕，不会去覆盖别人的锁。</p>
 *
 * <p>注意：Redis 分布式锁为“尽力而为”的互斥，非严格可重入。需要强一致时可再叠加数据库唯一约束
 * （如抽签任务表的 active 生成列）作为第二道闸门。</p>
 *
 * @author cjlu
 */
@Component
public class DistributedLock
{
    private static final Logger log = LoggerFactory.getLogger(DistributedLock.class);

    /** 锁键统一前缀，隔离业务键空间 */
    private static final String LOCK_PREFIX = "lock:";

    /** 释放锁脚本：仅当持有者 token 匹配时才删除，保证解锁安全 */
    private static final byte[] UNLOCK_SCRIPT = script(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");

    /** 续租脚本：仅当持有者 token 匹配时才重置租约，避免给别人的锁续命 */
    private static final byte[] RENEW_SCRIPT = script(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end");

    /** 续期间隔下限：租约配得极短时也不要把调度线程打满 */
    private static final long MIN_RENEW_INTERVAL_MS = 1000L;

    @Autowired
    private RedisConnectionFactory connectionFactory;

    /** 看门狗调度器：只做 PEXPIRE，无业务逻辑，独立守护线程，避免与业务线程池互相拖累 */
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "lock-watchdog");
        t.setDaemon(true);
        return t;
    });

    /**
     * 尝试获取锁（自旋重试至等待超时），**不自动续期**。
     *
     * @param key         业务锁键（内部自动加 lock: 前缀）
     * @param waitTimeMs  最长等待时间（毫秒），<=0 表示只尝试一次
     * @param leaseTimeMs 锁租约（毫秒），到期自动释放以防死锁
     * @return 获取成功返回持有者 token（解锁时需回传），失败返回 {@code null}
     */
    public String tryLock(String key, long waitTimeMs, long leaseTimeMs)
    {
        String token = UUID.randomUUID().toString();
        return acquire(key, token, waitTimeMs, leaseTimeMs) ? token : null;
    }

    /**
     * 尝试获取锁并返回**自动续期**的句柄（T-5）。适合执行时长不确定的批量任务，
     * 调用方在 finally / try-with-resources 中关闭句柄即可，无需估算任务要跑多久。
     *
     * @param key         业务锁键
     * @param waitTimeMs  最长等待时间（毫秒），<=0 表示只尝试一次
     * @param leaseTimeMs 单次租约（毫秒）；句柄存活期间每 1/3 租约续期一次
     * @return 获取成功返回句柄，失败返回 {@code null}
     */
    public LockHandle tryAcquire(String key, long waitTimeMs, long leaseTimeMs)
    {
        String token = tryLock(key, waitTimeMs, leaseTimeMs);
        return token == null ? null : new LockHandle(key, token, leaseTimeMs);
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
        Long result = eval(UNLOCK_SCRIPT, bytes(LOCK_PREFIX + key), bytes(token));
        return result != null && result > 0;
    }

    /**
     * 续租（token 匹配才重置租约）。
     *
     * @param key         业务锁键
     * @param token       持有者 token
     * @param leaseTimeMs 新的租约（毫秒）
     * @return 是否续租成功；返回 false 说明锁已不再由本持有者掌握
     */
    public boolean renew(String key, String token, long leaseTimeMs)
    {
        if (token == null)
        {
            return false;
        }
        Long result = eval(RENEW_SCRIPT, bytes(LOCK_PREFIX + key), bytes(token), bytes(String.valueOf(leaseTimeMs)));
        return result != null && result > 0;
    }

    /* ==================== 内部实现 ==================== */

    private boolean acquire(String key, String token, long waitTimeMs, long leaseTimeMs)
    {
        byte[] lockKey = bytes(LOCK_PREFIX + key);
        byte[] lockValue = bytes(token);
        long deadline = System.currentTimeMillis() + Math.max(0L, waitTimeMs);
        do
        {
            if (setIfAbsent(lockKey, lockValue, leaseTimeMs))
            {
                return true;
            }
            if (System.currentTimeMillis() >= deadline)
            {
                return false;
            }
            try
            {
                // 短暂退避，避免高频轮询打满 Redis
                Thread.sleep(50L);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        while (System.currentTimeMillis() <= deadline);
        return false;
    }

    private boolean setIfAbsent(byte[] key, byte[] value, long leaseTimeMs)
    {
        RedisConnection connection = connectionFactory.getConnection();
        try
        {
            return Boolean.TRUE.equals(
                    connection.set(key, value, Expiration.milliseconds(leaseTimeMs), SetOption.SET_IF_ABSENT));
        }
        finally
        {
            close(connection);
        }
    }

    private Long eval(byte[] scriptBytes, byte[]... keysAndArgs)
    {
        RedisConnection connection = connectionFactory.getConnection();
        try
        {
            return connection.eval(scriptBytes, ReturnType.INTEGER, 1, keysAndArgs);
        }
        finally
        {
            close(connection);
        }
    }

    private void close(RedisConnection connection)
    {
        try
        {
            connection.close();
        }
        catch (Exception e)
        {
            // 归还连接失败不掩盖主流程结论，只留痕（连接泄漏由连接池自身监控兜底）
            log.warn("归还 Redis 连接失败：{}", e.getMessage());
        }
    }

    private static byte[] script(String text)
    {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String text)
    {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @PreDestroy
    public void shutdown()
    {
        watchdog.shutdownNow();
    }

    /**
     * 自动续期的锁句柄（T-5）：持有期间按租约的 1/3 定时 {@code PEXPIRE}，
     * 关闭时先停止续期再按 token 解锁。
     */
    public class LockHandle implements AutoCloseable
    {
        private final String key;

        private final String token;

        private final long leaseTimeMs;

        private final ScheduledFuture<?> renewFuture;

        private volatile boolean released;

        private LockHandle(String key, String token, long leaseTimeMs)
        {
            this.key = key;
            this.token = token;
            this.leaseTimeMs = leaseTimeMs;
            long interval = Math.max(MIN_RENEW_INTERVAL_MS, leaseTimeMs / 3);
            this.renewFuture = watchdog.scheduleWithFixedDelay(this::renewQuietly, interval, interval,
                    TimeUnit.MILLISECONDS);
        }

        public String getKey()
        {
            return key;
        }

        public String getToken()
        {
            return token;
        }

        public long getLeaseTimeMs()
        {
            return leaseTimeMs;
        }

        private void renewQuietly()
        {
            if (released)
            {
                return;
            }
            try
            {
                if (!renew(key, token, leaseTimeMs))
                {
                    // 锁已不属于本持有者（任务超时、实例重启后被他人接管）：立即停止续期，绝不覆盖他人的锁
                    log.warn("分布式锁续期失败，锁已失效：key={}", key);
                    renewFuture.cancel(false);
                }
            }
            catch (Exception e)
            {
                // 单次续期抖动不影响持锁判定，下一轮会重试；连续失败由 TTL 兜底
                log.warn("分布式锁续期异常：key={}, cause={}", key, e.getMessage());
            }
        }

        @Override
        public void close()
        {
            released = true;
            renewFuture.cancel(false);
            unlock(key, token);
        }
    }
}
