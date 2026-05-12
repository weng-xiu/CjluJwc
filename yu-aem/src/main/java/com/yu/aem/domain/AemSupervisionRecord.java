package com.yu.aem.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 督导听课记录对象 aem_supervision_record
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemSupervisionRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 授课教师ID */
    @Excel(name = "授课教师ID")
    private Long teacherId;

    /** 督导姓名 */
    @Excel(name = "督导姓名")
    private String supervisor;

    /** 听课日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "听课日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date visitDate;

    /** 听课节数 */
    @Excel(name = "听课节数")
    private Integer classHours;

    /** 教学内容 */
    @Excel(name = "教学内容")
    private String teachingContent;

    /** 评价评分 */
    @Excel(name = "评价评分")
    private Double evaluationScore;

    /** 评价等级（0优秀 1良好 2合格 3不合格） */
    @Excel(name = "评价等级", readConverterExp = "0=优秀,1=良好,2=合格,3=不合格")
    private String evaluationLevel;

    /** 改进建议 */
    @Excel(name = "改进建议")
    private String suggestion;

    /** 记录类型（0常规听课 1专项督导 2反馈复查） */
    @Excel(name = "记录类型", readConverterExp = "0=常规听课,1=专项督导,2=反馈复查")
    private String recordType;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    @NotBlank(message = "督导姓名不能为空")
    @Size(min = 0, max = 50, message = "督导姓名长度不能超过50个字符")
    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public Date getVisitDate() { return visitDate; }
    public void setVisitDate(Date visitDate) { this.visitDate = visitDate; }

    public Integer getClassHours() { return classHours; }
    public void setClassHours(Integer classHours) { this.classHours = classHours; }

    public String getTeachingContent() { return teachingContent; }
    public void setTeachingContent(String teachingContent) { this.teachingContent = teachingContent; }

    public Double getEvaluationScore() { return evaluationScore; }
    public void setEvaluationScore(Double evaluationScore) { this.evaluationScore = evaluationScore; }

    public String getEvaluationLevel() { return evaluationLevel; }
    public void setEvaluationLevel(String evaluationLevel) { this.evaluationLevel = evaluationLevel; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("courseId", getCourseId())
            .append("teacherId", getTeacherId())
            .append("supervisor", getSupervisor())
            .append("visitDate", getVisitDate())
            .append("classHours", getClassHours())
            .append("teachingContent", getTeachingContent())
            .append("evaluationScore", getEvaluationScore())
            .append("evaluationLevel", getEvaluationLevel())
            .append("suggestion", getSuggestion())
            .append("recordType", getRecordType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
