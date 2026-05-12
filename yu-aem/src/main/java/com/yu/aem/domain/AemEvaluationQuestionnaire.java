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
 * 评教问卷配置对象 aem_evaluation_questionnaire
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemEvaluationQuestionnaire extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 问卷ID */
    private Long questionnaireId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 问卷标题 */
    @Excel(name = "问卷标题")
    private String title;

    /** 问卷说明 */
    @Excel(name = "问卷说明")
    private String description;

    /** 题目数量 */
    @Excel(name = "题目数量")
    private Integer questionCount;

    /** 满分 */
    @Excel(name = "满分")
    private Double fullScore;

    /** 评教开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "评教开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 评教结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "评教结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 评教状态（0未开始 1进行中 2已结束） */
    @Excel(name = "评教状态", readConverterExp = "0=未开始,1=进行中,2=已结束")
    private String evalStatus;

    /** 是否匿名（0实名 1匿名） */
    @Excel(name = "是否匿名", readConverterExp = "0=实名,1=匿名")
    private String isAnonymous;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(Long questionnaireId) { this.questionnaireId = questionnaireId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotBlank(message = "问卷标题不能为空")
    @Size(min = 0, max = 200, message = "问卷标题长度不能超过200个字符")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public Double getFullScore() { return fullScore; }
    public void setFullScore(Double fullScore) { this.fullScore = fullScore; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getEvalStatus() { return evalStatus; }
    public void setEvalStatus(String evalStatus) { this.evalStatus = evalStatus; }

    public String getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(String isAnonymous) { this.isAnonymous = isAnonymous; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("questionnaireId", getQuestionnaireId())
            .append("semesterId", getSemesterId())
            .append("title", getTitle())
            .append("description", getDescription())
            .append("questionCount", getQuestionCount())
            .append("fullScore", getFullScore())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("evalStatus", getEvalStatus())
            .append("isAnonymous", getIsAnonymous())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
