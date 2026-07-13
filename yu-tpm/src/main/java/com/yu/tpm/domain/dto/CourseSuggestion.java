package com.yu.tpm.domain.dto;

import java.io.Serializable;

/**
 * 替代课程建议
 *
 * @author ruoyi
 */
public class CourseSuggestion implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 开课ID */
    private Long courseOfferingId;

    /** 课程名称 */
    private String courseName;

    /** 教师名称 */
    private String teacherName;

    /** 上课时间描述 */
    private String scheduleDesc;

    /** 学分 */
    private Double credit;

    /** 容量上限 */
    private Integer maxStudents;

    /** 已选人数 */
    private Integer enrolledCount;

    public CourseSuggestion()
    {
    }

    public Long getCourseOfferingId() { return courseOfferingId; }
    public void setCourseOfferingId(Long courseOfferingId) { this.courseOfferingId = courseOfferingId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getScheduleDesc() { return scheduleDesc; }
    public void setScheduleDesc(String scheduleDesc) { this.scheduleDesc = scheduleDesc; }

    public Double getCredit() { return credit; }
    public void setCredit(Double credit) { this.credit = credit; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public Integer getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(Integer enrolledCount) { this.enrolledCount = enrolledCount; }
}
