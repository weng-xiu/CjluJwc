package com.yu.aem.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 成绩记录对象 aem_grade_record
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemGradeRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成绩ID */
    private Long gradeId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 考试类型（0正考 1补考 2重修） */
    @Excel(name = "考试类型", readConverterExp = "0=正考,1=补考,2=重修")
    private String examType;

    /** 平时成绩 */
    @Excel(name = "平时成绩")
    private Double regularScore;

    /** 考试成绩 */
    @Excel(name = "考试成绩")
    private Double examScore;

    /** 总成绩 */
    @Excel(name = "总成绩")
    private Double totalScore;

    /** 绩点 */
    @Excel(name = "绩点")
    private Double gradePoint;

    /** 等级 */
    @Excel(name = "等级", readConverterExp = "A=优秀,B=良好,C=中等,D=及格,F=不及格")
    private String gradeLevel;

    /** 是否通过（0否 1是） */
    @Excel(name = "是否通过", readConverterExp = "0=否,1=是")
    private String isPass;

    /** 是否已复核（0未复核 1已复核） */
    @Excel(name = "是否已复核", readConverterExp = "0=未复核,1=已复核")
    private String isReviewed;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotNull(message = "课程ID不能为空")
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    @NotNull(message = "学期ID不能为空")
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public Double getRegularScore() { return regularScore; }
    public void setRegularScore(Double regularScore) { this.regularScore = regularScore; }

    public Double getExamScore() { return examScore; }
    public void setExamScore(Double examScore) { this.examScore = examScore; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public Double getGradePoint() { return gradePoint; }
    public void setGradePoint(Double gradePoint) { this.gradePoint = gradePoint; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getIsPass() { return isPass; }
    public void setIsPass(String isPass) { this.isPass = isPass; }

    public String getIsReviewed() { return isReviewed; }
    public void setIsReviewed(String isReviewed) { this.isReviewed = isReviewed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("gradeId", getGradeId())
            .append("studentId", getStudentId())
            .append("courseId", getCourseId())
            .append("semesterId", getSemesterId())
            .append("examType", getExamType())
            .append("regularScore", getRegularScore())
            .append("examScore", getExamScore())
            .append("totalScore", getTotalScore())
            .append("gradePoint", getGradePoint())
            .append("gradeLevel", getGradeLevel())
            .append("isPass", getIsPass())
            .append("isReviewed", getIsReviewed())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
