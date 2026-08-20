package com.yu.tpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.tpm.domain.TpmSchedule;

/**
 * 排课Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmScheduleMapper 
{
    public TpmSchedule selectTpmScheduleByScheduleId(Long scheduleId);
    public List<TpmSchedule> selectTpmScheduleList(TpmSchedule tpmSchedule);
    public int insertTpmSchedule(TpmSchedule tpmSchedule);
    public int updateTpmSchedule(TpmSchedule tpmSchedule);
    public int deleteTpmScheduleByScheduleId(Long scheduleId);
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds);

    /** 按学期查询所有排课（含关联信息） */
    List<TpmSchedule> selectSchedulesBySemester(@Param("semesterId") Long semesterId);

    /** 查询某教室指定时间段的排课 */
    List<TpmSchedule> selectByClassroomAndTime(@Param("classroomId") Long classroomId,
                                               @Param("weekDay") Integer weekDay,
                                               @Param("startPeriod") Integer startPeriod,
                                               @Param("endPeriod") Integer endPeriod);

    /** 查询未分配教室的排课 */
    List<TpmSchedule> selectUnassignedSchedules(@Param("semesterId") Long semesterId);

    /**
     * 根据多个开课ID批量查询排课信息
     *
     * @param offeringIds 开课ID列表
     * @return 排课列表
     */
    public List<TpmSchedule> selectByOfferingIds(@Param("offeringIds") List<Long> offeringIds);

    /**
     * 根据教室和时间范围查询排课（用于教室占用检测）
     *
     * @param classroomId 教室ID
     * @param weekDay     星期几
     * @param startPeriod 开始节次
     * @param endPeriod   结束节次
     * @return 排课列表
     */
    public List<TpmSchedule> selectByClassroomAndTimeRange(@Param("classroomId") Long classroomId, @Param("weekDay") Integer weekDay, @Param("startPeriod") Integer startPeriod, @Param("endPeriod") Integer endPeriod);

    /**
     * 查询某教师在指定星期几和节次有重叠的排课（用于教师冲突校验）
     *
     * @param teacherId   教师ID
     * @param weekDay     星期几
     * @param startPeriod 开始节次
     * @param endPeriod   结束节次
     * @return 排课列表
     */
    public List<TpmSchedule> selectByTeacherAndTime(@Param("teacherId") Long teacherId,
                                                    @Param("weekDay") Integer weekDay,
                                                    @Param("startPeriod") Integer startPeriod,
                                                    @Param("endPeriod") Integer endPeriod);
}
