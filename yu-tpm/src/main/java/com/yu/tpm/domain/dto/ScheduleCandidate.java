package com.yu.tpm.domain.dto;

/**
 * 自动排课候选（T1）：一条待排课的开课计划 + 其课程学时信息。
 * 由 selectOfferingsToSchedule 查询装配，供排课引擎决定时间片与教室。
 *
 * @author ruoyi
 */
public class ScheduleCandidate
{
    /** 开课ID */
    private Long offeringId;

    /** 课程ID */
    private Long courseId;

    /** 课程名称 */
    private String courseName;

    /** 教师ID */
    private Long teacherId;

    /** 教师姓名 */
    private String teacherName;

    /** 期望校区ID（跨校区软约束用） */
    private Long campusId;

    /** 容量上限（选课人数） */
    private Integer maxStudents;

    /** 教学班数 */
    private Integer classCount;

    /** 总学时 */
    private Integer totalHours;

    /** 实践学时（>0 视为需实验/机房类教室） */
    private Integer practiceHours;

    public Long getOfferingId() { return offeringId; }
    public void setOfferingId(Long offeringId) { this.offeringId = offeringId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public Integer getClassCount() { return classCount; }
    public void setClassCount(Integer classCount) { this.classCount = classCount; }

    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }

    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }
}
