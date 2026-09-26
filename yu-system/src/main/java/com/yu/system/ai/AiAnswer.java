package com.yu.system.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能问答应答结果（Phase34 AI应用试点）
 *
 * 回答口径必须显式披露：answerSource 标明答案来自大模型、本地知识库抽取还是未命中，
 * references 给出依据条目，便于用户核对，避免把模型生成内容当作权威结论。
 *
 * @author yu
 * @date 2026-09-26
 */
public class AiAnswer implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 回答来源：大模型生成 */
    public static final String SOURCE_LLM = "LLM";

    /** 回答来源：本地知识库抽取 */
    public static final String SOURCE_EXTRACT = "EXTRACT";

    /** 回答来源：知识库未命中 */
    public static final String SOURCE_NONE = "NONE";

    /** 回答来源：大模型调用失败，已降级为抽取式回答 */
    public static final String SOURCE_ERROR = "ERROR";

    /** 问题原文 */
    private String question;

    /** 回答内容 */
    private String answer;

    /** 回答来源（LLM/EXTRACT/NONE/ERROR） */
    private String answerSource;

    /** 检索置信度（最高命中得分，0~1） */
    private Double confidence;

    /** 耗时（毫秒） */
    private Integer costTime;

    /** 引用依据（命中的知识条目摘要） */
    private List<AiKnowledgeHit> references = new ArrayList<>();

    /** 大模型可用状态说明（供前端展示当前是否已接入模型） */
    private String engineNote;

    /** 失败或降级原因 */
    private String errorMsg;

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

    public Integer getCostTime()
    {
        return costTime;
    }

    public void setCostTime(Integer costTime)
    {
        this.costTime = costTime;
    }

    public List<AiKnowledgeHit> getReferences()
    {
        return references;
    }

    public void setReferences(List<AiKnowledgeHit> references)
    {
        this.references = references;
    }

    public String getEngineNote()
    {
        return engineNote;
    }

    public void setEngineNote(String engineNote)
    {
        this.engineNote = engineNote;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }
}
