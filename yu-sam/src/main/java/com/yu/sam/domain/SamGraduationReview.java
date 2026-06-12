package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 毕业资格审核对象 sam_graduation_review
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamGraduationReview extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 审核ID */
    private Long reviewId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 已获总学分 */
    @Excel(name = "已获总学分")
    private Double totalCreditsEarned;

    /** 要求学分 */
    @Excel(name = "要求学分")
    private Double requiredCredits;

    /** 学分是否合格（0否 1是） */
    @Excel(name = "学分是否合格", readConverterExp = "0=否,1=是")
    private String isCreditQualified;

    /** 课程是否合格（0否 1是） */
    @Excel(name = "课程是否合格", readConverterExp = "0=否,1=是")
    private String isCourseQualified;

    /** 英语是否合格（0否 1是） */
    @Excel(name = "英语是否合格", readConverterExp = "0=否,1=是")
    private String isEnglishQualified;

    /** 体育是否合格（0否 1是） */
    @Excel(name = "体育是否合格", readConverterExp = "0=否,1=是")
    private String isPeQualified;

    /** 审核状态（0待审 1通过 2不通过） */
    @Excel(name = "审核状态", readConverterExp = "0=待审,1=通过,2=不通过")
    private String reviewStatus;

    /** 审核日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "审核日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date reviewDate;

    /** 审核人 */
    @Excel(name = "审核人")
    private String reviewer;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String reviewOpinion;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Double getTotalCreditsEarned() { return totalCreditsEarned; }
    public void setTotalCreditsEarned(Double totalCreditsEarned) { this.totalCreditsEarned = totalCreditsEarned; }

    public Double getRequiredCredits() { return requiredCredits; }
    public void setRequiredCredits(Double requiredCredits) { this.requiredCredits = requiredCredits; }

    public String getIsCreditQualified() { return isCreditQualified; }
    public void setIsCreditQualified(String isCreditQualified) { this.isCreditQualified = isCreditQualified; }

    public String getIsCourseQualified() { return isCourseQualified; }
    public void setIsCourseQualified(String isCourseQualified) { this.isCourseQualified = isCourseQualified; }

    public String getIsEnglishQualified() { return isEnglishQualified; }
    public void setIsEnglishQualified(String isEnglishQualified) { this.isEnglishQualified = isEnglishQualified; }

    public String getIsPeQualified() { return isPeQualified; }
    public void setIsPeQualified(String isPeQualified) { this.isPeQualified = isPeQualified; }

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public Date getReviewDate() { return reviewDate; }
    public void setReviewDate(Date reviewDate) { this.reviewDate = reviewDate; }

    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }

    public String getReviewOpinion() { return reviewOpinion; }
    public void setReviewOpinion(String reviewOpinion) { this.reviewOpinion = reviewOpinion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("reviewId", getReviewId())
            .append("studentId", getStudentId())
            .append("totalCreditsEarned", getTotalCreditsEarned())
            .append("requiredCredits", getRequiredCredits())
            .append("isCreditQualified", getIsCreditQualified())
            .append("isCourseQualified", getIsCourseQualified())
            .append("isEnglishQualified", getIsEnglishQualified())
            .append("isPeQualified", getIsPeQualified())
            .append("reviewStatus", getReviewStatus())
            .append("reviewDate", getReviewDate())
            .append("reviewer", getReviewer())
            .append("reviewOpinion", getReviewOpinion())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
