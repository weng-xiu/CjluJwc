package com.yu.system.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * AI问答留痕记录 sys_ai_chat_record（Phase34 AI应用试点）
 *
 * 每一次问答都落库，用于回答口径审计、知识库命中分析与「未命中问题」运营：
 * answer_source 区分大模型生成、本地抽取与未命中，confidence 记录检索置信度。
 *
 * @author yu
 * @date 2026-09-26
 */
public class SysAiChatRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 提问用户ID */
    private Long userId;

    /** 提问用户登录名 */
    @Excel(name = "提问用户")
    private String userName;

    /** 提问用户角色标识 */
    private String userRole;

    /** 提问场景（portal门户自助 admin后台自测） */
    @Excel(name = "场景")
    private String scene;

    /** 问题原文 */
    @Excel(name = "问题")
    private String question;

    /** 回答内容 */
    private String answer;

    /** 回答来源（LLM大模型 EXTRACT本地抽取 NONE未命中 ERROR调用失败已降级） */
    @Excel(name = "回答来源")
    private String answerSource;

    /** 命中知识ID */
    private String knowledgeIds;

    /** 命中知识标题 */
    private String knowledgeTitles;

    /** 检索置信度 */
    @Excel(name = "置信度")
    private Double confidence;

    /** 耗时（毫秒） */
    @Excel(name = "耗时(ms)")
    private Integer costTime;

    /** 状态（0成功 1失败） */
    private String status;

    /** 失败/降级原因 */
    private String errorMsg;

    /** 查询条件：开始时间 */
    private String beginTime;

    /** 查询条件：结束时间 */
    private String endTime;

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getRecordId()
    {
        return recordId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserRole(String userRole)
    {
        this.userRole = userRole;
    }

    public String getUserRole()
    {
        return userRole;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }

    public String getScene()
    {
        return scene;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getQuestion()
    {
        return question;
    }

    public void setAnswer(String answer)
    {
        this.answer = answer;
    }

    public String getAnswer()
    {
        return answer;
    }

    public void setAnswerSource(String answerSource)
    {
        this.answerSource = answerSource;
    }

    public String getAnswerSource()
    {
        return answerSource;
    }

    public void setKnowledgeIds(String knowledgeIds)
    {
        this.knowledgeIds = knowledgeIds;
    }

    public String getKnowledgeIds()
    {
        return knowledgeIds;
    }

    public void setKnowledgeTitles(String knowledgeTitles)
    {
        this.knowledgeTitles = knowledgeTitles;
    }

    public String getKnowledgeTitles()
    {
        return knowledgeTitles;
    }

    public void setConfidence(Double confidence)
    {
        this.confidence = confidence;
    }

    public Double getConfidence()
    {
        return confidence;
    }

    public void setCostTime(Integer costTime)
    {
        this.costTime = costTime;
    }

    public Integer getCostTime()
    {
        return costTime;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setBeginTime(String beginTime)
    {
        this.beginTime = beginTime;
    }

    public String getBeginTime()
    {
        return beginTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }

    public String getEndTime()
    {
        return endTime;
    }
}
