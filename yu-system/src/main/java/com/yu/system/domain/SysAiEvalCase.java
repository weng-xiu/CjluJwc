package com.yu.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 智能体评测用例 sys_ai_eval_case（K4 AI 治理 · 发布前评测集 golden set）
 *
 * 当前系统的 AI 是单一 RAG 教务政策问答助手，无多智能体/发布态，故「智能体发布」口径收敛为
 * 「助手所依据的知识库/提示词一次更新」。发布前须以本评测集回归：每条用例给定提问与期望命中，
 * 由 SysAiEvalServiceImpl 逐条驱动问答并打分，未达通过率阈值即拦截发布，形成能力评估留档。
 *
 * 命中判定：期望命中知识ID(knowledgeIds) 或 期望得分关键词(expectKeywords) 任一满足即视为该用例通过，
 * 两者可只填其一；均为空时以「检索命中且置信度≥阈值」作为通过口径。
 *
 * @author yu
 * @date 2026-10-12
 */
public class SysAiEvalCase extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用例ID */
    @Excel(name = "用例编号")
    private Long caseId;

    /** 评测提问 */
    @Excel(name = "评测提问")
    private String question;

    /** 期望命中知识ID（逗号分隔，可空） */
    private String expectKnowledgeIds;

    /** 期望得分关键词（逗号/分号分隔，回答须包含即命中，可空） */
    @Excel(name = "期望关键词")
    private String expectKeywords;

    /** 业务分类 */
    @Excel(name = "业务分类")
    private String category;

    /** 排序 */
    private Integer sort;

    /** 是否启用（0启用 1停用） */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String enabled;

    public Long getCaseId()
    {
        return caseId;
    }

    public void setCaseId(Long caseId)
    {
        this.caseId = caseId;
    }

    @NotBlank(message = "评测提问不能为空")
    @Size(min = 0, max = 500, message = "评测提问不能超过500个字符")
    public String getQuestion()
    {
        return question;
    }

    public void setQuestion(String question)
    {
        this.question = question;
    }

    public String getExpectKnowledgeIds()
    {
        return expectKnowledgeIds;
    }

    public void setExpectKnowledgeIds(String expectKnowledgeIds)
    {
        this.expectKnowledgeIds = expectKnowledgeIds;
    }

    public String getExpectKeywords()
    {
        return expectKeywords;
    }

    public void setExpectKeywords(String expectKeywords)
    {
        this.expectKeywords = expectKeywords;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public Integer getSort()
    {
        return sort;
    }

    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }
}
