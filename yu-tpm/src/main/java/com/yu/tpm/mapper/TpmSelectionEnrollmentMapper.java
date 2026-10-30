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
     * @param enrollId   选课记录ID
     * @param updateTime 更新时间
     * @return 影响行数
     */
    public int promoteFromWaitlist(@Param("enrollId") Long enrollId, @Param("updateTime") java.util.Date updateTime);
}
