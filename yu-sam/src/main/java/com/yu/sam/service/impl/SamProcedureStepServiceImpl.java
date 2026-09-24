package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamProcedureStepMapper;
import com.yu.sam.domain.SamProcedureStep;
import com.yu.sam.service.ISamProcedureStepService;

/**
 * 离校环节配置Service业务层处理（S7c）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@Service
public class SamProcedureStepServiceImpl implements ISamProcedureStepService
{
    @Autowired
    private SamProcedureStepMapper samProcedureStepMapper;

    @Override
    public SamProcedureStep selectSamProcedureStepByStepId(Long stepId) { return samProcedureStepMapper.selectSamProcedureStepByStepId(stepId); }

    @Override
    public List<SamProcedureStep> selectSamProcedureStepList(SamProcedureStep samProcedureStep) { return samProcedureStepMapper.selectSamProcedureStepList(samProcedureStep); }

    @Override
    @Transactional
    public int insertSamProcedureStep(SamProcedureStep samProcedureStep)
    {
        if (samProcedureStep.getRequiredFlag() == null) samProcedureStep.setRequiredFlag("1");
        if (samProcedureStep.getAutoCheckType() == null) samProcedureStep.setAutoCheckType("NONE");
        if (samProcedureStep.getStatus() == null) samProcedureStep.setStatus("0");
        if (samProcedureStepMapper.countByStepKey(samProcedureStep.getStepKey(), null) > 0)
        {
            throw new ServiceException("环节编码 " + samProcedureStep.getStepKey() + " 已存在");
        }
        samProcedureStep.setCreateTime(DateUtils.getNowDate());
        return samProcedureStepMapper.insertSamProcedureStep(samProcedureStep);
    }

    @Override
    @Transactional
    public int updateSamProcedureStep(SamProcedureStep samProcedureStep)
    {
        if (samProcedureStep.getStepKey() != null
                && samProcedureStepMapper.countByStepKey(samProcedureStep.getStepKey(), samProcedureStep.getStepId()) > 0)
        {
            throw new ServiceException("环节编码 " + samProcedureStep.getStepKey() + " 已被其他环节占用");
        }
        samProcedureStep.setUpdateTime(DateUtils.getNowDate());
        return samProcedureStepMapper.updateSamProcedureStep(samProcedureStep);
    }

    @Override
    @Transactional
    public int deleteSamProcedureStepByStepIds(Long[] stepIds) { return samProcedureStepMapper.deleteSamProcedureStepByStepIds(stepIds); }
}
