package com.yu.system.mapper;

import java.util.List;

import com.yu.system.domain.SysAiEvalCase;

/**
 * 智能体评测用例 数据层（K4 AI 治理 · 发布前评测集）
 *
 * @author yu
 * @date 2026-10-12
 */
public interface SysAiEvalCaseMapper
{
    /** 查询用例列表 */
    public List<SysAiEvalCase> selectEvalCaseList(SysAiEvalCase evalCase);

    /** 按主键查询用例 */
    public SysAiEvalCase selectEvalCaseById(Long caseId);

    /** 查询全部启用用例（评测引擎按此跑分） */
    public List<SysAiEvalCase> selectEnabledList();

    /** 新增用例 */
    public int insertEvalCase(SysAiEvalCase evalCase);

    /** 修改用例 */
    public int updateEvalCase(SysAiEvalCase evalCase);

    /** 批量删除用例 */
    public int deleteEvalCaseByIds(Long[] caseIds);
}
