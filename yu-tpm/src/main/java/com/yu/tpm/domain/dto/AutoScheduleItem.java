package com.yu.tpm.domain.dto;

/**
 * 自动排课产出条目（T1）：引擎为某开课的某一次周课分配的时间片与教室。
 * 预览(dryRun)时直接返回给前端展示，落库时逐条写入 tpm_schedule。
 *
 * @author ruoyi
 */
public class AutoScheduleItem
{
    /** 开课ID */
    private Long offeringId;

    /** 课程名称 */
    private String courseName;

    /** 教师姓名 */
    private String teacherName;

    /** 星期几（1-7） */
    private Integer weekDay;

    /** 开始节次 */
    private Integer startPeriod;

    /** 结束节次 */
    private Integer endPeriod;

    /** 起始周 */
    private Integer startWeek;

    /** 结束周 */
    private Integer endWeek;

    /** 教室ID */
    private Long classroomId;

    /** 教室名称 */
    private String classroomName;

    /** 选择该教室的原因摘要（类型/容量/校区匹配） */
    private String note;

    public Long getOfferingId() { return offeringId; }
    public void setOfferingId(Long offeringId) { this.offeringId = offeringId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

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

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
