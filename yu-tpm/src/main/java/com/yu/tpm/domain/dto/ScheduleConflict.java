package com.yu.tpm.domain.dto;

import java.io.Serializable;

/**
 * 排课冲突DTO
 *
 * @author ruoyi
 */
public class ScheduleConflict implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 第一个排课ID */
    private Long scheduleId1;

    /** 第二个排课ID */
    private Long scheduleId2;

    /** 冲突类型：CLASSROOM_CONFLICT / TEACHER_CONFLICT / STUDENT_CONFLICT */
    private String conflictType;

    /** 冲突描述 */
    private String message;

    /** 第一门课程名 */
    private String courseName1;

    /** 第二门课程名 */
    private String courseName2;

    /** 教室名 */
    private String classroomName;

    /** 冲突时间描述 */
    private String timeDesc;

    public Long getScheduleId1() { return scheduleId1; }
    public void setScheduleId1(Long scheduleId1) { this.scheduleId1 = scheduleId1; }

    public Long getScheduleId2() { return scheduleId2; }
    public void setScheduleId2(Long scheduleId2) { this.scheduleId2 = scheduleId2; }

    public String getConflictType() { return conflictType; }
    public void setConflictType(String conflictType) { this.conflictType = conflictType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCourseName1() { return courseName1; }
    public void setCourseName1(String courseName1) { this.courseName1 = courseName1; }

    public String getCourseName2() { return courseName2; }
    public void setCourseName2(String courseName2) { this.courseName2 = courseName2; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getTimeDesc() { return timeDesc; }
    public void setTimeDesc(String timeDesc) { this.timeDesc = timeDesc; }
}
