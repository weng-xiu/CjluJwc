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
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
