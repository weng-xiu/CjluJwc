package com.yu.aem.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 成绩复核审批对象 aem_grade_review
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemGradeReview extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 复核ID */
    private Long reviewId;

    /** 成绩ID */
    @Excel(name = "成绩ID")
    private Long gradeId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 原成绩 */
    @Excel(name = "原成绩")
    private Double originalScore;

    /** 新成绩 */
    @Excel(name = "新成绩")
    private Double newScore;

    /** 复核原因 */
    @Excel(name = "复核原因")
    private String reviewReason;

    /** 复核类型（0成绩修改 1成绩复核） */
    @Excel(name = "复核类型", readConverterExp = "0=成绩修改,1=成绩复核")
    private String reviewType;

    /** 审批状态（0待审/院系初审中 1通过 2驳回 3已撤销 4待教务处终审） */
    @Excel(name = "审批状态", readConverterExp = "0=待审,1=通过,2=驳回,3=已撤销,4=待教务处终审")
    private String approveStatus;

    /** 审批人 */
    @Excel(name = "审批人")
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 审批意见 */
    @Excel(name = "审批意见")
    private String approveOpinion;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** O1：Flowable 流程实例ID（提交进流程后写入） */
    private String procInstId;

    /** O1：课程负责人初审人 */
    private String deptApproveBy;

    /** O1：课程负责人初审时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deptApproveTime;

    /** O1：课程负责人初审意见 */
    private String deptOpinion;

    /** O1：教务处终审人 */
    private String aaApproveBy;

    /** O1：教务处终审时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date aaApproveTime;

    /** O1：教务处终审意见 */
    private String aaOpinion;

    public String getProcInstId() { return procInstId; }
    public void setProcInstId(String procInstId) { this.procInstId = procInstId; }

    public String getDeptApproveBy() { return deptApproveBy; }
    public void setDeptApproveBy(String deptApproveBy) { this.deptApproveBy = deptApproveBy; }

    public Date getDeptApproveTime() { return deptApproveTime; }
    public void setDeptApproveTime(Date deptApproveTime) { this.deptApproveTime = deptApproveTime; }

    public String getDeptOpinion() { return deptOpinion; }
    public void setDeptOpinion(String deptOpinion) { this.deptOpinion = deptOpinion; }

    public String getAaApproveBy() { return aaApproveBy; }
    public void setAaApproveBy(String aaApproveBy) { this.aaApproveBy = aaApproveBy; }

    public Date getAaApproveTime() { return aaApproveTime; }
    public void setAaApproveTime(Date aaApproveTime) { this.aaApproveTime = aaApproveTime; }

    public String getAaOpinion() { return aaOpinion; }
    public void setAaOpinion(String aaOpinion) { this.aaOpinion = aaOpinion; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    @NotNull(message = "成绩ID不能为空")
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotNull(message = "课程ID不能为空")
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Double getOriginalScore() { return originalScore; }
    public void setOriginalScore(Double originalScore) { this.originalScore = originalScore; }

    public Double getNewScore() { return newScore; }
    public void setNewScore(Double newScore) { this.newScore = newScore; }

    @NotBlank(message = "复核原因不能为空")
    @Size(min = 0, max = 500, message = "复核原因长度不能超过500个字符")
    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }

    public String getReviewType() { return reviewType; }
    public void setReviewType(String reviewType) { this.reviewType = reviewType; }

    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }

    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }

    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }

    @Size(min = 0, max = 500, message = "审批意见长度不能超过500个字符")
    public String getApproveOpinion() { return approveOpinion; }
    public void setApproveOpinion(String approveOpinion) { this.approveOpinion = approveOpinion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reviewId", getReviewId())
            .append("gradeId", getGradeId())
            .append("studentId", getStudentId())
            .append("courseId", getCourseId())
            .append("originalScore", getOriginalScore())
            .append("newScore", getNewScore())
            .append("reviewReason", getReviewReason())
            .append("reviewType", getReviewType())
            .append("approveStatus", getApproveStatus())
            .append("approveBy", getApproveBy())
            .append("approveTime", getApproveTime())
            .append("approveOpinion", getApproveOpinion())
            .append("status", getStatus())
            .append("procInstId", getProcInstId())
            .append("deptApproveBy", getDeptApproveBy())
            .append("aaApproveBy", getAaApproveBy())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
