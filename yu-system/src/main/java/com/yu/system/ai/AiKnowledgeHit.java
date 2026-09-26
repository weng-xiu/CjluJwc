package com.yu.system.ai;

import java.io.Serializable;
import com.yu.system.domain.SysAiKnowledge;

/**
 * 知识库检索命中项（Phase34 AI应用试点）
 *
 * @author yu
 * @date 2026-09-26
 */
public class AiKnowledgeHit implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 命中的知识条目 */
    private SysAiKnowledge knowledge;

    /** 检索得分（TF-IDF 余弦相似度 + 关键词整词命中加成，取值 0~1） */
    private Double score;

    /** 命中说明（便于向用户披露「为什么推荐这条」，可解释性要求） */
    private String matchedOn;

    public AiKnowledgeHit()
    {
    }

    public AiKnowledgeHit(SysAiKnowledge knowledge, Double score, String matchedOn)
    {
        this.knowledge = knowledge;
        this.score = score;
        this.matchedOn = matchedOn;
    }

    public Long getKnowledgeId()
    {
        return knowledge == null ? null : knowledge.getKnowledgeId();
    }

    public String getTitle()
    {
        return knowledge == null ? null : knowledge.getTitle();
    }

    public SysAiKnowledge getKnowledge()
    {
        return knowledge;
    }

    public void setKnowledge(SysAiKnowledge knowledge)
    {
        this.knowledge = knowledge;
    }

    public Double getScore()
    {
        return score;
    }

    public void setScore(Double score)
    {
        this.score = score;
    }

    public String getMatchedOn()
    {
        return matchedOn;
    }

    public void setMatchedOn(String matchedOn)
    {
        this.matchedOn = matchedOn;
    }
}
