package com.yu.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教务政策AI知识库 sys_ai_knowledge（Phase34 AI应用试点）
 *
 * 智能问答的唯一事实来源：检索命中后由本地抽取式作答或注入大模型上下文，
 * keywords 用于提高口语化提问的召回率，source 记录依据出处以便追溯。
 *
 * @author yu
 * @date 2026-09-26
 */
public class SysAiKnowledge extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 知识ID */
    private Long knowledgeId;

    /** 分类（字典 sys_ai_category） */
    @Excel(name = "分类")
    private String category;

    /** 标准问题/条目标题 */
    @Excel(name = "条目名称")
    private String title;

    /** 检索关键词（逗号分隔） */
    private String keywords;

    /** 解答正文 */
    private String content;

    /** 依据来源 */
    @Excel(name = "依据来源")
    private String source;

    /** 参考链接 */
    private String refUrl;

    /** 累计命中次数 */
    @Excel(name = "命中次数")
    private Integer hitCount;

    /** 展示顺序 */
    private Integer orderNum;

    /** 状态（0启用 1停用） */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    /** 查询用：正文摘要（列表不返回大字段时填充） */
    private String summary;

    public void setKnowledgeId(Long knowledgeId)
    {
        this.knowledgeId = knowledgeId;
    }

    public Long getKnowledgeId()
    {
        return knowledgeId;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    @NotBlank(message = "知识分类不能为空")
    public String getCategory()
    {
        return category;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @NotBlank(message = "条目名称不能为空")
    @Size(min = 0, max = 200, message = "条目名称长度不能超过200个字符")
    public String getTitle()
    {
        return title;
    }

    public void setKeywords(String keywords)
    {
        this.keywords = keywords;
    }

    public String getKeywords()
    {
        return keywords;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    @NotBlank(message = "解答正文不能为空")
    public String getContent()
    {
        return content;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getSource()
    {
        return source;
    }

    public void setRefUrl(String refUrl)
    {
        this.refUrl = refUrl;
    }

    public String getRefUrl()
    {
        return refUrl;
    }

    public void setHitCount(Integer hitCount)
    {
        this.hitCount = hitCount;
    }

    public Integer getHitCount()
    {
        return hitCount;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getSummary()
    {
        return summary;
    }
}
