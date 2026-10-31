package com.yu.tpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.tpm.domain.TpmSelectionEnrollment;

/**
 * 选课名单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmSelectionEnrollmentMapper 
{
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);

    /**
     * P-3：抽签结果批量落库（单条 CASE WHEN 更新三列，按 Service 侧切片调用）。
     *
     * @param list       同一批次的选课记录，必须均已填好 lotteryResult / resultStatus / waitlistRank
     * @param updateTime 统一更新时间
     * @return 受影响行数
     */
    public int batchUpdateLotteryResult(@Param("list") List<TpmSelectionEnrollment> list,
                                        @Param("updateTime") java.util.Date updateTime);
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds);

    /**
     * 查询某开课的报名行数（含落选/候补，result_status != '3'）。
     *
     * <p>仅供「是否已有报名记录/删除前校验」使用：落选/候补（result_status='2'）也是报名痕迹，
     * 开课不能被删成悬空引用。**容量判定不得用它**，改用 {@link #countAdmittedByOffering(Long)}。</p>
     *
     * @param courseOfferingId 开课ID
     * @return 报名行数
     */
    public int selectCountByOffering(@Param("courseOfferingId") Long courseOfferingId);

    /**
     * 统计某开课的**在册人数**（result_status = '1'），与 tpm_course_offering.enrolled_count 同口径。
     *
     * <p>T-3：落选/候补（result_status='2'）没有占到名额，不能计入容量；用 != '3' 口径会把
     * 候补算满，导致“明明还有名额却提示已满”。</p>
     *
     * <p>与后面的 {@link #countAdmittedByOffering(Long)} 不是一回事：那个只数**抽签中签**
     * （lottery_result='1'），直接选课（lottery_result='0'）的人不在内；容量判定必须用本方法。</p>
     *
     * @param courseOfferingId 开课ID
     * @return 在册人数
     */
    public int countEnrolledByOffering(@Param("courseOfferingId") Long courseOfferingId);

    /**
     * 查询学生在某轮次**在册**的选课记录（result_status = '1'）。
     *
     * <p>抽签落选/候补（result_status='2'）不占选课门数，也不参与时间/学分冲突检测，
     * 因此这里不能返回它们，否则学生抽签后会因“已达到门数上限”而无法再补选。</p>
     *
     * @param studentId 学生ID
     * @param roundId   轮次ID
     * @return 在册选课记录列表
     */
    public List<TpmSelectionEnrollment> selectByStudentAndRound(@Param("studentId") Long studentId, @Param("roundId") Long roundId);

    /**
     * 统计学生已完成（选中）的指定课程数量（先修课校验用）
     *
     * @param studentId 学生ID
     * @param courseIds 课程ID列表
     * @return 已完成的课程门数
     */
    public int countCompletedCourses(@Param("studentId") Long studentId, @Param("courseIds") List<Long> courseIds);

    /**
     * T6：查询某开课的候补队列（抽签落选记录），按递补序号升序。
     *
     * @param courseOfferingId 开课ID
     * @return 候补选课记录列表
     */
    public List<TpmSelectionEnrollment> selectWaitlistByOffering(@Param("courseOfferingId") Long courseOfferingId);

    /**
     * T6：统计某开课抽签中签人数。
     *
     * @param courseOfferingId 开课ID
     * @return 中签人数
     */
    public int countAdmittedByOffering(@Param("courseOfferingId") Long courseOfferingId);

    /**
     * T6：将候补记录递补为中签（清空候补排名）。
     *
     * <p>T-3 并发收口：WHERE 增加 {@code result_status = '2'} 守卫。原先只按 enroll_id 更新，
     * 两个并发退课各触发一次递补时，同一条候补行会被“改两次”：名额被占两次而记录只升一次，
     * 剩下的名额永久泄漏。返回 0 表示该行已被并发请求递补/退掉，调用方必须归还刚占的名额。</p>
     *
     * @param enrollId   选课记录ID
     * @param updateTime 更新时间
     * @return 影响行数（1=递补成功；0=该行已不是候补态）
     */
    public int promoteFromWaitlist(@Param("enrollId") Long enrollId, @Param("updateTime") java.util.Date updateTime);

    /**
     * T-3 并发收口：退课的**条件状态迁移**（在册 '1' → 退课 '3'）。
     *
     * <p>原实现「先 select 判 result_status 再 update」，同一记录被并发退课时两条请求都读到 '1'，
     * 各自 releaseSeat 一次 → {@code enrolled_count} 被重复扣减；{@code releaseSeat} 的 {@code > 0}
     * 守卫只能防负数，防不住少计。把状态判定写进 WHERE 后，只有真正完成迁移的那一条返回 1，
     * 调用方仅在 affected=1 时释放名额。</p>
     *
     * @param enrollId 选课记录ID
     * @param dropTime 退课时间（同时写入 update_time）
     * @return 影响行数（1=本次完成迁移；0=记录已不是在册态或已删除）
     */
    public int dropIfEnrolled(@Param("enrollId") Long enrollId, @Param("dropTime") java.util.Date dropTime);

    /**
     * T-3 并发收口：改选的**条件状态迁移**（旧开课 → 新开课）。
     *
     * <p>{@code course_offering_id = 旧开课} 是并发栅栏：两个并发改选都通过冲突检测后，
     * 只有实际把「旧开课 → 新开课」迁移成功的那一条释放旧名额，否则会出现两次
     * occupySeat(新) 与两次 releaseSeat(旧)，名额与记录对不上。</p>
     *
     * @param enrollId      选课记录ID
     * @param oldOfferingId 记录当前指向的开课ID（栅栏条件）
     * @param newOfferingId 目标开课ID
     * @param updateTime    更新时间（同时写入 select_time）
     * @return 影响行数（1=本次完成迁移；0=记录已被并发改走/退掉）
     */
    public int swapIfEnrolled(@Param("enrollId") Long enrollId,
                              @Param("oldOfferingId") Long oldOfferingId,
                              @Param("newOfferingId") Long newOfferingId,
                              @Param("updateTime") java.util.Date updateTime);
}
