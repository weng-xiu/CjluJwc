package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 学位资格审核对象 sam_degree_review
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamDegreeReview extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 审核ID */
    private Long reviewId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 毕业审核ID */
    @Excel(name = "毕业审核ID")
    private Long graduationReviewId;

    /** 平均绩点 */
    @Excel(name = "平均绩点")
    private Double gpa;

    /** 绩点是否合格（0否 1是） */
    @Excel(name = "绩点是否合格", readConverterExp = "0=否,1=是")
    private String isGpaQualified;

    /** 学位课程是否合格（0否 1是） */
    @Excel(name = "学位课程是否合格", readConverterExp = "0=否,1=是")
    private String isDegreeCourseQualified;

    /** 论文是否合格（0否 1是） */
    @Excel(name = "论文是否合格", readConverterExp = "0=否,1=是")
    private String isThesisQualified;

    /** 学位类型 */
    @Excel(name = "学位类型")
    private String degreeType;

    /** 学位学科门类 */
    @Excel(name = "学位学科门类")
    private String degreeField;

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

    public Long getGraduationReviewId() { return graduationReviewId; }
    public void setGraduationReviewId(Long graduationReviewId) { this.graduationReviewId = graduationReviewId; }

    public Double getGpa() { return gpa; }
    public void setGpa(Double gpa) { this.gpa = gpa; }

    public String getIsGpaQualified() { return isGpaQualified; }
    public void setIsGpaQualified(String isGpaQualified) { this.isGpaQualified = isGpaQualified; }

    public String getIsDegreeCourseQualified() { return isDegreeCourseQualified; }
    public void setIsDegreeCourseQualified(String isDegreeCourseQualified) { this.isDegreeCourseQualified = isDegreeCourseQualified; }

    public String getIsThesisQualified() { return isThesisQualified; }
    public void setIsThesisQualified(String isThesisQualified) { this.isThesisQualified = isThesisQualified; }

    public String getDegreeType() { return degreeType; }
    public void setDegreeType(String degreeType) { this.degreeType = degreeType; }

    public String getDegreeField() { return degreeField; }
    public void setDegreeField(String degreeField) { this.degreeField = degreeField; }

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
            .append("graduationReviewId", getGraduationReviewId())
            .append("gpa", getGpa())
            .append("isGpaQualified", getIsGpaQualified())
            .append("isDegreeCourseQualified", getIsDegreeCourseQualified())
            .append("isThesisQualified", getIsThesisQualified())
            .append("degreeType", getDegreeType())
            .append("degreeField", getDegreeField())
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
