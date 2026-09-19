package com.yu.tpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.StudentScheduleSlot;

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

    /** 门户端：按学生查询本人课表（选课记录→开课→排课，可选学期过滤） */
    public List<TpmSchedule> selectStudentScheduleList(@Param("studentId") Long studentId,
                                                        @Param("semesterId") Long semesterId);
    public int insertTpmSchedule(TpmSchedule tpmSchedule);
    public int updateTpmSchedule(TpmSchedule tpmSchedule);
    public int deleteTpmScheduleByScheduleId(Long scheduleId);
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds);

    /** 按学期查询所有排课（含关联信息） */
    List<TpmSchedule> selectSchedulesBySemester(@Param("semesterId") Long semesterId);

    /**
     * 按学期查询"学生-课表时间槽"列表（选课名单→开课→排课），
     * 用于按学生名单精确判定班级/学生冲突。包含未分配教室的排课。
     *
     * @param semesterId 学期ID
     * @return 学生课表时间槽列表
     */
    List<StudentScheduleSlot> selectStudentScheduleSlotsBySemester(@Param("semesterId") Long semesterId);

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
