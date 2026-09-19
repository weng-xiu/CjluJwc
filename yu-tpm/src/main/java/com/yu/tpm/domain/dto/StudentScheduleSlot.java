package com.yu.tpm.domain.dto;

import java.io.Serializable;

/**
 * 学生课表时间槽（用于按学生名单精确判定班级/学生冲突）。
 * 表示"某学生所选某开课的一条排课时间"。
 *
 * @author ruoyi
 */
public class StudentScheduleSlot implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 学生ID */
    private Long studentId;

    /** 开课ID */
    private Long offeringId;

    /** 排课ID */
    private Long scheduleId;

    /** 星期几（1-7） */
    private Integer weekDay;

    private Integer startPeriod;
    private Integer endPeriod;
    private Integer startWeek;
    private Integer endWeek;

    /** 课程名称 */
    private String courseName;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getOfferingId() { return offeringId; }
    public void setOfferingId(Long offeringId) { this.offeringId = offeringId; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Integer getWeekDay() { return weekDay; }
    public void setWeekDay(Integer weekDay) { this.weekDay = weekDay; }

    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }

    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }

    public Integer getStartWeek() { return startWeek; }
    public void setStartWeek(Integer startWeek) { this.startWeek = startWeek; }

    public Integer getEndWeek() { return endWeek; }
    public void setEndWeek(Integer endWeek) { this.endWeek = endWeek; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
}
