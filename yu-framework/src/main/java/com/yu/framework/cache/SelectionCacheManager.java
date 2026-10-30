package com.yu.framework.cache;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 选课缓存管理器
 * 管理选课轮次信息、学生已选课程等缓存
 *
 * <p>T-3：课程容量不再进缓存。容量的唯一真相是 tpm_course_offering.enrolled_count，
 * 由 DB 条件更新（occupySeat / releaseSeat / recountEnrolled）维护。原先的 sel:capacity:*
 * 键在“准入改为 DB 条件更新”后已无任何消费方，留在代码里就是会与库不一致的冗余计数，
 * 因此整组下线（decrementCapacity / incrementCapacity / setCapacity / getCapacity /
 * clearCapacityCache）。历史键随 TTL 自然过期，无需主动清理。</p>
 *
 * @author ruoyi
 */
@Component
public class SelectionCacheManager
{
    /** 选课轮次缓存键前缀 */
    private static final String SEL_ROUND_KEY = "sel:round:";

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
}
