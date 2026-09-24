package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamProcedureStep;

/**
 * 离校环节配置Service接口（S7c）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public interface ISamProcedureStepService
{
    public SamProcedureStep selectSamProcedureStepByStepId(Long stepId);

    public List<SamProcedureStep> selectSamProcedureStepList(SamProcedureStep samProcedureStep);

    public int insertSamProcedureStep(SamProcedureStep samProcedureStep);

    public int updateSamProcedureStep(SamProcedureStep samProcedureStep);

    public int deleteSamProcedureStepByStepIds(Long[] stepIds);
}
