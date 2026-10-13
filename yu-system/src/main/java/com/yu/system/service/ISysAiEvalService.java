package com.yu.system.service;

import java.util.List;

import com.yu.system.domain.SysAiEvalCase;
import com.yu.system.domain.SysAiEvalResult;
import com.yu.system.domain.SysAiEvalRun;

/**
 * 智能体发布前评测 服务层（K4 AI 治理 · 评测集存档 + 发布门禁）
 *
 * 把当前单一 RAG 问答助手视为「智能体」，发布/更新前以其 golden 评测集回归跑分并留痕，
 * 通过率低于阈值即判定拦截。评测批次与逐条结果为审计数据，只写不删。
 *
 * @author yu
 * @date 2026-10-12
 */
public interface ISysAiEvalService
{
    /** 查询评测用例列表 */
    public List<SysAiEvalCase> selectEvalCaseList(SysAiEvalCase evalCase);

    /** 查询评测用例 */
    public SysAiEvalCase selectEvalCaseById(Long caseId);

    /** 新增评测用例 */
    public int insertEvalCase(SysAiEvalCase evalCase);

    /** 修改评测用例 */
    public int updateEvalCase(SysAiEvalCase evalCase);

    /** 批量删除评测用例 */
    public int deleteEvalCaseByIds(Long[] caseIds);

    /**
     * 以全部启用用例驱动问答并打分留痕。
     *
     * @param triggerType 触发方式（MANUAL/PUBLISH）
     * @param operator    操作人登录名
     * @return 评测批次（含通过率与门禁结果），逐条结果已落库
     */
    public SysAiEvalRun evaluate(String triggerType, String operator);

    /** 查询评测批次列表 */
    public List<SysAiEvalRun> selectEvalRunList(SysAiEvalRun run);

    /** 查询评测批次 */
    public SysAiEvalRun selectEvalRunById(Long runId);

    /** 查询某批次的逐条评测结果 */
    public List<SysAiEvalResult> selectResultsByRunId(Long runId);
}
