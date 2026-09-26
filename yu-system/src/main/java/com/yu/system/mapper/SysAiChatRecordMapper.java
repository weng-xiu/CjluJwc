package com.yu.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.system.domain.SysAiChatRecord;

/**
 * AI问答留痕记录 数据层
 *
 * 问答留痕属审计数据，只开放写入与查询，不提供删除口径。
 *
 * @author yu
 * @date 2026-09-26
 */
public interface SysAiChatRecordMapper
{
    /**
     * 查询问答记录
     *
     * @param recordId 记录ID
     * @return 问答记录
     */
    public SysAiChatRecord selectSysAiChatRecordByRecordId(Long recordId);

    /**
     * 查询问答记录列表
     *
     * @param sysAiChatRecord 问答记录
     * @return 问答记录集合
     */
    public List<SysAiChatRecord> selectSysAiChatRecordList(SysAiChatRecord sysAiChatRecord);

    /**
     * 新增问答记录
     *
     * @param sysAiChatRecord 问答记录
     * @return 结果
     */
    public int insertSysAiChatRecord(SysAiChatRecord sysAiChatRecord);

    /**
     * 总览指标（提问量、命中量、命中率、平均置信度、平均耗时、提问人数）
     *
     * @param sysAiChatRecord 查询条件（beginTime/endTime/scene）
     * @return 总览指标
     */
    public Map<String, Object> selectOverviewStat(SysAiChatRecord sysAiChatRecord);

    /**
     * 按回答来源分布统计
     *
     * @param sysAiChatRecord 查询条件
     * @return 来源分布
     */
    public List<Map<String, Object>> selectSourceStat(SysAiChatRecord sysAiChatRecord);

    /**
     * 按提问场景分布统计
     *
     * @param sysAiChatRecord 查询条件
     * @return 场景分布
     */
    public List<Map<String, Object>> selectSceneStat(SysAiChatRecord sysAiChatRecord);

    /**
     * 近期提问与命中趋势（按日）
     *
     * @param days 统计天数
     * @return 趋势数据
     */
    public List<Map<String, Object>> selectDailyTrend(@Param("days") int days);

    /**
     * 未命中问题清单（知识库补齐线索）
     *
     * @param limit 条数
     * @return 未命中问题
     */
    public List<Map<String, Object>> selectUnmatchedQuestions(@Param("limit") int limit);
}
