package com.yu.tpm.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 开课计划对象 tpm_course_offering
 *
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmCourseOffering extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 开课ID */
    private Long offeringId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 教师ID */
    @Excel(name = "教师ID")
    private Long teacherId;

    /** 校区ID */
    @Excel(name = "校区ID")
    private Long campusId;

    /** 教学班数 */
    @Excel(name = "教学班数")
    private Integer classCount;

    /** 容量上限 */
    @Excel(name = "容量上限")
    private Integer maxStudents;

    /** 开课状态（0待确认 1已确认 2已取消） */
    @Excel(name = "开课状态", readConverterExp = "0=待确认,1=已确认,2=已取消")
    private String offeringStatus;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    // ========== 以下为关联查询的冗余字段，不映射到数据库 ==========

    /** 学期名称（关联查询） */
    private String semesterName;

    /** 课程名称（关联查询） */
    private String courseName;

    /** 课程编码（关联查询） */
    private String courseCode;

    /** 教师姓名（关联查询） */
    private String teacherName;

    /** 校区名称（关联查询） */
    private String campusName;

    public Long getOfferingId() { return offeringId; }
    public void setOfferingId(Long offeringId) { this.offeringId = offeringId; }

    @NotNull(message = "学期ID不能为空")
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotNull(message = "课程ID不能为空")
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Integer getClassCount() { return classCount; }
    public void setClassCount(Integer classCount) { this.classCount = classCount; }

    public Integer getMaxStudents() { return maxStudents; }
    public void setMaxStudents(Integer maxStudents) { this.maxStudents = maxStudents; }

    public String getOfferingStatus() { return offeringStatus; }
    public void setOfferingStatus(String offeringStatus) { this.offeringStatus = offeringStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("offeringId", getOfferingId())
            .append("semesterId", getSemesterId())
            .append("courseId", getCourseId())
            .append("teacherId", getTeacherId())
            .append("campusId", getCampusId())
            .append("classCount", getClassCount())
            .append("maxStudents", getMaxStudents())
            .append("offeringStatus", getOfferingStatus())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
