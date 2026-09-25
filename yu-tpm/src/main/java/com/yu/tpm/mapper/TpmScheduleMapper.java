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

    /**
     * 查询指定开课的学生在同窗口时段所选其他开课的排课时间槽（T5 拖拽调整的学生冲突定位查询）。
     * 语义：选了该开课(result_status='1')的学生，在本学期其他开课中、与目标 (星期/节次/周次) 窗口
     * 时间重叠且同星期几的课表槽，用于拖拽落点前精确判定学生/班级冲突。
     *
     * @param offeringId   被调整的开课ID
     * @param semesterId   学期ID
     * @param weekDay      目标星期几（1-7）
     * @param startPeriod  目标开始节次
     * @param endPeriod    目标结束节次
     * @param startWeek    目标起始周
     * @param endWeek      目标结束周
     * @return 存在重叠的学生课表时间槽列表
     */
    List<StudentScheduleSlot> selectStudentSlotsByOfferingInWindow(@Param("offeringId") Long offeringId,
                                                                   @Param("semesterId") Long semesterId,
                                                                   @Param("weekDay") Integer weekDay,
                                                                   @Param("startPeriod") Integer startPeriod,
                                                                   @Param("endPeriod") Integer endPeriod,
                                                                   @Param("startWeek") Integer startWeek,
                                                                   @Param("endWeek") Integer endWeek);

    /** 查询某教室指定时间段的排课 */
    List<TpmSchedule> selectByClassroomAndTime(@Param("classroomId") Long classroomId,
                                               @Param("weekDay") Integer weekDay,
                                               @Param("startPeriod") Integer startPeriod,
                                               @Param("endPeriod") Integer endPeriod);

    /** T5 拖拽调整：查询某教室指定时间段的排课（含课程名/教师名，用于冲突明细） */
    List<TpmSchedule> selectByClassroomAndTimeWithInfo(@Param("classroomId") Long classroomId,
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

    /**
     * 查询可自动排课的教室（T1）：状态正常，含楼宇/校区/类型信息，用于容量、类型匹配与跨校区软约束。
     *
     * @return 可排课教室列表
     */
    java.util.List<com.yu.tpm.domain.dto.SchedulableClassroom> selectSchedulableClassrooms();
}
