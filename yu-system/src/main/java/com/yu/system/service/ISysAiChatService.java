package com.yu.system.service;

import java.util.List;
import java.util.Map;
import com.yu.system.ai.AiAnswer;
import com.yu.system.domain.SysAiChatRecord;

/**
 * AI 智能问答 服务层（Phase34 AI应用试点）
 *
 * 问答留痕为审计数据，本服务只提供写入与查询，不开放删除。
 *
 * @author yu
 * @date 2026-09-26
 */
public interface ISysAiChatService
{
    /**
     * 回答教务政策问题，并落库留痕
     *
     * @param question 问题原文
     * @param topK     引用条目数，小于等于 0 时取参数 ai.qa.topK
     * @param scene    提问场景（portal/admin）
     * @param userId   提问用户ID
     * @param userName 提问用户登录名
     * @param userRole 提问用户角色标识
     * @return 应答结果（含回答口径与引用依据）
     */
    public AiAnswer ask(String question, int topK, String scene, Long userId, String userName, String userRole);

    /**
     * 回答教务政策问题（提问人取自当前登录上下文）
     *
     * @param question 问题原文
     * @param topK     引用条目数，小于等于 0 时取参数 ai.qa.topK
     * @param scene    提问场景（portal/admin）
     * @return 应答结果
     */
    public AiAnswer ask(String question, int topK, String scene);

    /**
     * 查询问答记录列表
     *
     * @param sysAiChatRecord 问答记录
     * @return 问答记录集合
     */
    public List<SysAiChatRecord> selectSysAiChatRecordList(SysAiChatRecord sysAiChatRecord);

    /**
     * 查询问答记录
     *
     * @param recordId 记录ID
     * @return 问答记录
     */
    public SysAiChatRecord selectSysAiChatRecordByRecordId(Long recordId);

    /**
     * 问答效果统计（总览、来源分布、场景分布、按日趋势、未命中问题）
     *
     * @param sysAiChatRecord 查询条件
     * @param trendDays       趋势统计天数
     * @param unmatchedLimit  未命中问题条数
     * @return 统计结果
     */
    public Map<String, Object> selectChatStat(SysAiChatRecord sysAiChatRecord, int trendDays, int unmatchedLimit);

    /**
     * 推荐问法（取自知识库条目，供门户首页引导）
     *
     * @param limit 条数
     * @return 问题列表
     */
    public List<String> suggestQuestions(int limit);
}
