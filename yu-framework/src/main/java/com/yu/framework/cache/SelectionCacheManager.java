package com.yu.framework.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 选课缓存管理器
 * 管理选课轮次信息、课程容量、学生已选课程等缓存
 *
 * @author ruoyi
 */
@Component
public class SelectionCacheManager
{
    /** 选课轮次缓存键前缀 */
    private static final String SEL_ROUND_KEY = "sel:round:";

    /** 课程容量缓存键前缀 */
    private static final String SEL_CAPACITY_KEY = "sel:capacity:";

    /** 学生已选课程缓存键前缀 */
    private static final String SEL_STUDENT_KEY = "sel:student:";

    /** 选课轮次缓存过期时间（5分钟） */
    private static final long ROUND_TTL = 5;

    /** 学生选课缓存过期时间（30秒） */
    private static final long STUDENT_TTL = 30;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 获取选课轮次信息（TTL 5分钟）
     *
     * @param roundId 轮次ID
     * @return 轮次信息，不存在则返回null
     */
    public Object getRoundInfo(Long roundId)
    {
        String key = SEL_ROUND_KEY + roundId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 缓存选课轮次信息（TTL 5分钟）
     *
     * @param roundId   轮次ID
     * @param roundInfo 轮次信息
     */
    public void setRoundInfo(Long roundId, Object roundInfo)
    {
        String key = SEL_ROUND_KEY + roundId;
        redisTemplate.opsForValue().set(key, roundInfo, ROUND_TTL, TimeUnit.MINUTES);
    }

    /**
     * 原子扣减容量，返回剩余容量
     * 使用Redis的DECR命令保证原子性
     *
     * @param offeringId 开课ID
     * @return 剩余容量，小于0表示已满
     */
    public Long decrementCapacity(Long offeringId)
    {
        String key = SEL_CAPACITY_KEY + offeringId;
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 原子恢复容量（退课时调用）
     * 使用Redis的INCR命令保证原子性
     *
     * @param offeringId 开课ID
     * @return 恢复后的容量
     */
    public Long incrementCapacity(Long offeringId)
    {
        String key = SEL_CAPACITY_KEY + offeringId;
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 设置课程容量
     *
     * @param offeringId 开课ID
     * @param capacity   容量上限
     */
    public void setCapacity(Long offeringId, int capacity)
    {
        String key = SEL_CAPACITY_KEY + offeringId;
        redisTemplate.opsForValue().set(key, capacity);
    }

    /**
     * 获取课程剩余容量
     *
     * @param offeringId 开课ID
     * @return 剩余容量，不存在则返回null
     */
    public Long getCapacity(Long offeringId)
    {
        String key = SEL_CAPACITY_KEY + offeringId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    /**
     * 获取学生已选课程（TTL 30秒）
     *
     * @param studentId 学生ID
     * @param roundId   轮次ID
     * @return 已选开课ID列表，不存在则返回null
     */
    @SuppressWarnings("unchecked")
    public List<Long> getStudentSelections(Long studentId, Long roundId)
    {
        String key = SEL_STUDENT_KEY + studentId + ":" + roundId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null)
        {
            return null;
        }
        if (value instanceof List)
        {
            return (List<Long>) value;
        }
        return null;
    }

    /**
     * 缓存学生已选课程（TTL 30秒）
     *
     * @param studentId   学生ID
     * @param roundId     轮次ID
     * @param offeringIds 已选开课ID列表
     */
    public void setStudentSelections(Long studentId, Long roundId, List<Long> offeringIds)
    {
        String key = SEL_STUDENT_KEY + studentId + ":" + roundId;
        redisTemplate.opsForValue().set(key, offeringIds, STUDENT_TTL, TimeUnit.SECONDS);
    }

    /**
     * 清除学生选课缓存
     *
     * @param studentId 学生ID
     * @param roundId   轮次ID
     */
    public void clearStudentCache(Long studentId, Long roundId)
    {
        String key = SEL_STUDENT_KEY + studentId + ":" + roundId;
        redisTemplate.delete(key);
    }

    /**
     * 清除选课轮次缓存
     *
     * @param roundId 轮次ID
     */
    public void clearRoundCache(Long roundId)
    {
        String key = SEL_ROUND_KEY + roundId;
        redisTemplate.delete(key);
    }

    /**
     * 清除课程容量缓存
     *
     * @param offeringId 开课ID
     */
    public void clearCapacityCache(Long offeringId)
    {
        String key = SEL_CAPACITY_KEY + offeringId;
        redisTemplate.delete(key);
    }
}
