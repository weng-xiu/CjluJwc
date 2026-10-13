package com.yu.system.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 智能体评测逐条结果 sys_ai_eval_result（K4 AI 治理 · 发布前打分留痕明细）
 *
 * 每个评测用例在一次批次中的实际作答与判定：命中口径、置信度、是否通过，供发布前复盘与算法审计。
 *
 * @author yu
 * @date 2026-10-12
 */
public class SysAiEvalResult extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 结果ID */
    private Long resultId;

    /** 所属批次ID */
    private Long runId;

    /** 用例ID */
    @Excel(name = "用例编号")
    private Long caseId;

    /** 评测提问 */
    @Excel(name = "评测提问")
    private String question;

    /** 实际回答 */
    @Excel(name = "实际回答")
    private String answer;

    /** 回答来源（LLM/EXTRACT/NONE/ERROR/MODERATED） */
    @Excel(name = "回答来源", dictType = "sys_ai_answer_source")
    private String answerSource;

    /** 检索置信度 */
    @Excel(name = "置信度")
    private Double confidence;

    /** 内容治理结果码（0通过 1输入拦截 2输出改写） */
    @Excel(name = "治理结果", dictType = "sys_ai_moderation_result")
    private String moderationResult;

    /** 是否命中期望（0命中 1未命中） */
    @Excel(name = "命中", readConverterExp = "0=命中,1=未命中")
    private String matched;

    /** 该用例是否通过（0通过 1未过） */
    @Excel(name = "用例结果", readConverterExp = "0=通过,1=未过")
    private String passed;

    public Long getResultId()
    {
        return resultId;
    }

    public void setResultId(Long resultId)
    {
        this.resultId = resultId;
    }

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Long getCaseId()
    {
        return caseId;
    }

    public void setCaseId(Long caseId)
    {
        this.caseId = caseId;
    }

    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getAnswerSource()
    {
        return answerSource;
    }

    public void setAnswerSource(String answerSource)
    {
        this.answerSource = answerSource;
    }

    public Double getConfidence()
    {
        return confidence;
    }

    public void setConfidence(Double confidence)
    {
        this.confidence = confidence;
    }

    public String getModerationResult()
    {
        return moderationResult;
    }

    public void setModerationResult(String moderationResult)
    {
        this.moderationResult = moderationResult;
    }

    public String getMatched()
    {
        return matched;
    }

    public void setMatched(String matched)
    {
        this.matched = matched;
    }

    public String getPassed()
    {
        return passed;
    }

    public void setPassed(String passed)
    {
        this.passed = passed;
    }
}
