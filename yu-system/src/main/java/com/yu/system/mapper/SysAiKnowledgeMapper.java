package com.yu.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.system.domain.SysAiKnowledge;

/**
 * 教务政策AI知识库 数据层
 *
 * @author yu
 * @date 2026-09-26
 */
public interface SysAiKnowledgeMapper
{
    /**
     * 查询知识条目
     *
     * @param knowledgeId 知识ID
     * @return 知识条目
     */
    public SysAiKnowledge selectSysAiKnowledgeByKnowledgeId(Long knowledgeId);

    /**
     * 查询知识条目列表（列表态仅返回正文摘要，避免大字段拖慢分页）
     *
     * @param sysAiKnowledge 知识条目
     * @return 知识条目集合
     */
    public List<SysAiKnowledge> selectSysAiKnowledgeList(SysAiKnowledge sysAiKnowledge);

    /**
     * 查询全部启用条目（供检索索引构建使用，含正文全文）
     *
     * @return 启用条目集合
     */
    public List<SysAiKnowledge> selectEnabledList();

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
     * 删除知识条目
     *
     * @param knowledgeId 知识ID
     * @return 结果
     */
    public int deleteSysAiKnowledgeByKnowledgeId(Long knowledgeId);

    /**
     * 批量删除知识条目
     *
     * @param knowledgeIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysAiKnowledgeByKnowledgeIds(Long[] knowledgeIds);

    /**
     * 累加命中热度
     *
     * @param knowledgeIds 知识ID数组
     * @return 结果
     */
    public int increaseHitCount(@Param("knowledgeIds") Long[] knowledgeIds);

    /**
     * 按分类统计条目数与命中量（知识库治理面板）
     *
     * @return 分类统计
     */
    public List<Map<String, Object>> selectCategoryStat();

    /**
     * 命中量最高的知识条目
     *
     * @param limit 条数
     * @return 统计结果
     */
    public List<Map<String, Object>> selectHotKnowledge(@Param("limit") int limit);
}
