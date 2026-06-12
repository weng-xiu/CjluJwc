package com.yu.aem.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 评教问题对象 aem_evaluation_question
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemEvaluationQuestion extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 问题ID */
    private Long questionId;

    /** 问卷ID */
    @Excel(name = "问卷ID")
    private Long questionnaireId;

    /** 问题类型（0单选 1多选 2评分 3文本） */
    @Excel(name = "问题类型", readConverterExp = "0=单选,1=多选,2=评分,3=文本")
    private String questionType;

    /** 问题内容 */
    @Excel(name = "问题内容")
    private String questionContent;

    /** 排序号 */
    @Excel(name = "排序号")
    private Integer sortOrder;

    /** 最高评分 */
    @Excel(name = "最高评分")
    private Double maxScore;

    /** 选项JSON */
    @Excel(name = "选项JSON")
    private String optionsJson;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    @NotNull(message = "问卷ID不能为空")
    public Long getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(Long questionnaireId) { this.questionnaireId = questionnaireId; }

    @NotBlank(message = "问题类型不能为空")
    @Size(min = 0, max = 1, message = "问题类型长度不能超过1个字符")
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    @NotBlank(message = "问题内容不能为空")
    @Size(min = 0, max = 500, message = "问题内容长度不能超过500个字符")
    public String getQuestionContent() { return questionContent; }
    public void setQuestionContent(String questionContent) { this.questionContent = questionContent; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Double getMaxScore() { return maxScore; }
    public void setMaxScore(Double maxScore) { this.maxScore = maxScore; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("questionId", getQuestionId())
            .append("questionnaireId", getQuestionnaireId())
            .append("questionType", getQuestionType())
            .append("questionContent", getQuestionContent())
            .append("sortOrder", getSortOrder())
            .append("maxScore", getMaxScore())
            .append("optionsJson", getOptionsJson())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
