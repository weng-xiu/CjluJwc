package com.yu.system.service;

import java.util.List;
import java.util.Map;
import com.yu.system.ai.AiKnowledgeHit;
import com.yu.system.domain.SysAiKnowledge;

/**
 * 教务政策AI知识库 服务层
 *
 * @author yu
 * @date 2026-09-26
 */
public interface ISysAiKnowledgeService
{
    /**
     * 查询知识条目
     *
     * @param knowledgeId 知识ID
     * @return 知识条目
     */
    public SysAiKnowledge selectSysAiKnowledgeByKnowledgeId(Long knowledgeId);

    /**
     * 查询知识条目列表
     *
     * @param sysAiKnowledge 知识条目
     * @return 知识条目集合
     */
    public List<SysAiKnowledge> selectSysAiKnowledgeList(SysAiKnowledge sysAiKnowledge);

    /**
     * 查询全部启用条目（含正文，供检索索引与推荐问法使用）
     *
     * @return 启用条目集合
     */
    public List<SysAiKnowledge> listEnabled();

    /**
     * 检索与问题相关的知识条目（已按 ai.qa.minScore 过滤置信度）
     *
     * @param question 问题
     * @param topK     返回条数，小于等于 0 时取参数 ai.qa.topK
     * @return 命中项（得分降序）
     */
    public List<AiKnowledgeHit> retrieve(String question, int topK);

    /**
     * 问答检索最低置信度（ai.qa.minScore）
     *
     * @return 阈值
     */
    public double minScore();

    /**
     * 新增知识条目
     *
     * @param sysAiKnowledge 知识条目
     * @return 结果
     */
    public int insertSysAiKnowledge(SysAiKnowledge sysAiKnowledge);

    /**
     * 修改知识条目
     *
     * @param sysAiKnowledge 知识条目
     * @return 结果
     */
    public int updateSysAiKnowledge(SysAiKnowledge sysAiKnowledge);

    /**
     * 批量删除知识条目
     *
     * @param knowledgeIds 需要删除的知识ID
     * @return 结果
     */
    public int deleteSysAiKnowledgeByKnowledgeIds(Long[] knowledgeIds);

    /**
     * 累加命中热度
     *
     * @param knowledgeIds 知识ID集合
     */
    public void markHit(List<Long> knowledgeIds);

    /**
     * 按分类统计条目数与命中量
     *
     * @return 分类统计
     */
    public List<Map<String, Object>> selectCategoryStat();

    /**
     * 热门知识条目
     *
     * @param limit 条数
     * @return 统计结果
     */
    public List<Map<String, Object>> selectHotKnowledge(int limit);
}
