package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.dto.OfferingCapacityStat;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import org.apache.ibatis.annotations.Param;

/**
 * 开课计划Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmCourseOfferingMapper 
{
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId);
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering);

    /** 门户端：可选课程列表（附带学分/已选人数，无数据范围过滤） */
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering);
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId);
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds);

    /** 查询指定学期已存在（未删除且未取消）开课的课程ID集合，用于批量生成幂等去重 */
    public List<Long> selectActiveCourseIdsBySemester(@Param("semesterId") Long semesterId);

    /** 查询可用教师池（在职 status=0），可选按院系过滤 */
    public List<Long> selectTeacherPool(@Param("deptId") Long deptId);

    /** 批量插入开课计划 */
    public int batchInsertTpmCourseOffering(@Param("list") List<TpmCourseOffering> list);

    /**
     * 查询指定学期"待自动排课"的开课（T1）：已确认(offering_status='1')、正常、且尚无任何未删除排课记录，
     * 附带课程学时信息（总学时/实践学时）用于推导周课时与教室类型偏好。
     *
     * @param semesterId 学期ID
     * @return 排课候选列表
     */
    public List<ScheduleCandidate> selectOfferingsToSchedule(@Param("semesterId") Long semesterId);

    /**
     * N4 增量重排：按开课ID集合载入候选（含课程学时/校区/容量），用于对被腾空的开课重排。
     *
     * @param offeringIds 受影响的开课ID集合（非空）
     * @return 排课候选列表
     */
    public List<ScheduleCandidate> selectCandidatesByOfferingIds(@Param("offeringIds") List<Long> offeringIds);

    /**
     * P2 选课容量水位告警：统计指定轮次下各开课的已选中/候补人数。
     * 仅返回该轮次存在报名记录的开课，按已选人数降序（水位最紧张的在前）。
     *
     * @param roundId 轮次ID
     * @return 容量水位统计列表
     */
    public List<OfferingCapacityStat> selectRoundCapacityStats(@Param("roundId") Long roundId);

    /**
     * T-3：原子占用一个名额（容量真相在 DB）。
     *
     * <p>条件更新把「是否还有名额」与「占一个名额」压成同一条语句，并发选课由 InnoDB 行锁串行化，
     * 不存在「先查后改」的窗口。切勿改回「先 select 判余量再 update」——那会让并发请求同时通过判断。</p>
     *
     * @param offeringId 开课ID
     * @return 1=占到名额；0=已满（或开课不存在/已删除/容量未配置）
     */
    public int occupySeat(@Param("offeringId") Long offeringId);

    /**
     * T-3：释放一个名额（退课、改选回退、递补失败回滚）。
     * SQL 带 {@code enrolled_count > 0} 守卫，重复释放不会把计数减成负数。
     *
     * @param offeringId 开课ID
     * @return 受影响行数（0 表示计数已为 0 或开课不存在）
     */
    public int releaseSeat(@Param("offeringId") Long offeringId);

    /**
     * T-3：按报名表真相全量重算占用数（幂等）。
     *
     * <p>抽签会把部分「已选中」改为「落选/候补」，这种情况必须重算而不是逐条减；
     * 管理端直接增删改报名记录后也用它校正，避免计数漂移。</p>
     *
     * @param offeringIds 开课ID集合（非空）
     * @return 受影响行数
     */
    public int recountEnrolled(@Param("offeringIds") List<Long> offeringIds);
}
