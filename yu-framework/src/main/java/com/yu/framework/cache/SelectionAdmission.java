package com.yu.framework.cache;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * A1 选课尖峰削峰与排队号发号器。
 *
 * <p>选课开放瞬间的并发洪峰若直冲 DB，既有压垮连接池之虞，也让“超卖防护”缺乏前置节流证据。
 * 本组件在业务写路径之前插入一道 <b>Redis 令牌桶</b> 全局闸门：按轮次平滑放行，
 * 超限请求<b>不触达 DB</b>，仅领取一个单调递增的排队号后被快速拒绝，稍后凭号重试，
 * 从而实现削峰填谷与可归因的容量水位控制。</p>
 *
 * <p>令牌桶与发号在同一 Lua 脚本内原子完成，避免多实例竞态。参数经配置项可调，默认即可用：
 * {@code selection.admission.rate-per-second}（每秒放行数）、{@code selection.admission.burst}（桶容量）。
 * 键空间 TTL 60 秒，无尖峰时自动回收。</p>
 *
 * <p>注意：这是“尽力而为”的入口平滑，与下游的容量原子扣减、抽签分布式锁互补而非替代；
 * 放行与否不代表最终选课成功，仅决定是否进入后续真实处理。</p>
 *
 * @author cjlu
 */
@Component
public class SelectionAdmission
{
    /** 令牌桶键前缀 */
    private static final String BUCKET_KEY = "sel:admit:bucket:";
    /** 排队号发号计数键前缀 */
    private static final String SEQ_KEY = "sel:admit:seq:";

    /** 每秒放行令牌数（削峰速率），默认 100 */
    @Value("${selection.admission.rate-per-second:100}")
    private long ratePerSecond;

    /** 令牌桶容量（允许的瞬时突发），默认 200 */
    @Value("${selection.admission.burst:200}")
    private long burst;

    /** 桶/发号键的空闲回收时间（秒） */
    private static final long KEY_TTL_SECONDS = 60L;

    private static final DefaultRedisScript<List> ADMIT_SCRIPT;

    static
    {
        ADMIT_SCRIPT = new DefaultRedisScript<>();
        ADMIT_SCRIPT.setResultType(List.class);
        // KEYS[1]=桶, KEYS[2]=发号; ARGV[1]=rate/s ARGV[2]=burst ARGV[3]=now(ms) ARGV[4]=ttl(ms)
        ADMIT_SCRIPT.setScriptText(
                "local bucket = KEYS[1] " +
                "local seqKey = KEYS[2] " +
                "local rate = tonumber(ARGV[1]) " +
                "local burst = tonumber(ARGV[2]) " +
                "local now = tonumber(ARGV[3]) " +
                "local ttl = tonumber(ARGV[4]) " +
                "local state = redis.call('HMGET', bucket, 't', 'ts') " +
                "local tokens = tonumber(state[1]) " +
                "local ts = tonumber(state[2]) " +
                "if tokens == nil then tokens = burst; ts = now end " +
                "local elapsed = now - ts; if elapsed < 0 then elapsed = 0 end " +
                "tokens = math.min(burst, tokens + (elapsed / 1000.0) * rate) " +
                "local allowed = 0 " +
                "if tokens >= 1 then tokens = tokens - 1; allowed = 1 end " +
                "redis.call('HSET', bucket, 't', tokens, 'ts', now) " +
                "redis.call('PEXPIRE', bucket, ttl) " +
                "local seq = redis.call('INCR', seqKey) " +
                "redis.call('PEXPIRE', seqKey, ttl) " +
                "return {allowed, seq}");
    }

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 尝试为某轮次的选课请求放行。无论放行与否都会发放一个排队号（受理序号），供前端展示与审计。
     *
     * @param roundId 选课轮次ID（作为削峰与发号的隔离维度）
     * @return 放行结果；{@code allowed=true} 表示可进入真实选课处理，否则应稍后凭 {@code queueNumber} 重试
     */
    @SuppressWarnings("unchecked")
    public Admission tryAdmit(Long roundId)
    {
        if (roundId == null)
        {
            // 无轮次上下文时不设闸门，交由下游既有校验与原子扣减处理
            return new Admission(true, 0L);
        }
        try
        {
            Object raw = redisTemplate.execute(
                    ADMIT_SCRIPT,
                    Arrays.asList(BUCKET_KEY + roundId, SEQ_KEY + roundId),
                    String.valueOf(ratePerSecond), String.valueOf(burst),
                    String.valueOf(System.currentTimeMillis()), String.valueOf(KEY_TTL_SECONDS * 1000));
            List<?> result = (raw instanceof List) ? (List<?>) raw : null;
            if (result == null || result.size() < 2)
            {
                // 脚本异常时不阻断业务（降级放行），由容量原子扣减兜底防超卖
                return new Admission(true, 0L);
            }
            long allowed = ((Number) result.get(0)).longValue();
            long seq = ((Number) result.get(1)).longValue();
            return new Admission(allowed == 1L, seq);
        }
        catch (Exception e)
        {
            // Redis 故障降级：不阻断选课，避免削峰组件成为单点（超卖仍由下游原子扣减+锁防护）
            return new Admission(true, 0L);
        }
    }

    /** 放行结果 */
    public static class Admission
    {
        private final boolean allowed;
        private final long queueNumber;

        public Admission(boolean allowed, long queueNumber)
        {
            this.allowed = allowed;
            this.queueNumber = queueNumber;
        }

        public boolean isAllowed()
        {
            return allowed;
        }

        public long getQueueNumber()
        {
            return queueNumber;
        }
    }
}
