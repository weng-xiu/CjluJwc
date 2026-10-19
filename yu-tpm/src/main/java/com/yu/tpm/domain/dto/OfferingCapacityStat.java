package com.yu.tpm.domain.dto;

/**
 * 选课容量水位统计（P2）：进行中轮次下，一条有报名记录的开课的容量/已选中/候补情况。
 * 由 selectRoundCapacityStats 查询装配，供 SelectionCapacityTask 水位告警消费。
 *
 * @author ruoyi
 */
public class OfferingCapacityStat
{
    /** 开课ID */
    private Long offeringId;

    /** 课程名称 */
    private String courseName;

    /** 教师姓名 */
    private String teacherName;

    /** 容量上限（max_students，0/空视为未设限不参与水位判定） */
    private Integer maxStudents;

    /** 已选中人数（result_status='1'） */
    private Integer enrolled;

    /** 候补人数（落选且持有候补排名 result_status='2' & waitlist_rank 非空） */
    private Integer waitlisted;

    public Long getOfferingId()
    {
        return offeringId;
    }

    public void setOfferingId(Long offeringId)
    {
        this.offeringId = offeringId;
    }

    public String getCourseName()
    {
        return courseName;
    }

    public void setCourseName(String courseName)
    {
        this.courseName = courseName;
    }

    public String getTeacherName()
    {
        return teacherName;
    }

    public void setTeacherName(String teacherName)
    {
        this.teacherName = teacherName;
    }

    public Integer getMaxStudents()
    {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents)
    {
        this.maxStudents = maxStudents;
    }

    public Integer getEnrolled()
    {
        return enrolled;
    }

    public void setEnrolled(Integer enrolled)
    {
        this.enrolled = enrolled;
    }

    public Integer getWaitlisted()
    {
        return waitlisted;
    }

    public void setWaitlisted(Integer waitlisted)
    {
        this.waitlisted = waitlisted;
    }
}
