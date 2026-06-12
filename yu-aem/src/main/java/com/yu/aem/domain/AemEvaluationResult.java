package com.yu.aem.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 评教结果对象 aem_evaluation_result
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemEvaluationResult extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 结果ID */
    private Long resultId;

    /** 问卷ID */
    @Excel(name = "问卷ID")
    private Long questionnaireId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 教师ID */
    @Excel(name = "教师ID")
    private Long teacherId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 总评分 */
    @Excel(name = "总评分")
    private Double totalScore;

    /** 评教时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "评教时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date evalDate;

    /** 评语建议 */
    @Excel(name = "评语建议")
    private String comment;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getResultId() { return resultId; }
    public void setResultId(Long resultId) { this.resultId = resultId; }

    @NotNull(message = "问卷ID不能为空")
    public Long getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(Long questionnaireId) { this.questionnaireId = questionnaireId; }

    @NotNull(message = "课程ID不能为空")
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    @NotNull(message = "教师ID不能为空")
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public Date getEvalDate() { return evalDate; }
    public void setEvalDate(Date evalDate) { this.evalDate = evalDate; }

    @Size(min = 0, max = 500, message = "评语建议长度不能超过500个字符")
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("resultId", getResultId())
            .append("questionnaireId", getQuestionnaireId())
            .append("courseId", getCourseId())
            .append("teacherId", getTeacherId())
            .append("studentId", getStudentId())
            .append("totalScore", getTotalScore())
            .append("evalDate", getEvalDate())
            .append("comment", getComment())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
