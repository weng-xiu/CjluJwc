package com.yu.system.mapper;

import java.util.List;

import com.yu.system.domain.SysAiEvalResult;
import com.yu.system.domain.SysAiEvalRun;

/**
 * 智能体评测批次与逐条结果 数据层（K4 AI 治理 · 发布前打分留痕）
 *
 * 留痕为审计数据，只写入与查询，不开放删除。
 *
 * @author yu
 * @date 2026-10-12
 */
public interface SysAiEvalRunMapper
{
    /** 新增批次（回填自增 runId） */
    public int insertEvalRun(SysAiEvalRun run);

    /** 查询批次列表 */
    public List<SysAiEvalRun> selectEvalRunList(SysAiEvalRun run);

    /** 按主键查询批次 */
    public SysAiEvalRun selectEvalRunById(Long runId);

    /** 新增逐条结果（回填自增 resultId） */
    public int insertEvalResult(SysAiEvalResult result);

    /** 按批次查询逐条结果 */
    public List<SysAiEvalResult> selectResultsByRunId(Long runId);
}
