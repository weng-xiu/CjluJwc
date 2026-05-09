package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmTrainingPlanMapper;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.service.ITpmTrainingPlanService;

/**
 * 人才培养方案Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmTrainingPlanServiceImpl implements ITpmTrainingPlanService 
{
    @Autowired
    private TpmTrainingPlanMapper tpmTrainingPlanMapper;

    @Override
    public TpmTrainingPlan selectTpmTrainingPlanByPlanId(Long planId)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
    }

    @Override
    public List<TpmTrainingPlan> selectTpmTrainingPlanList(TpmTrainingPlan tpmTrainingPlan)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanList(tpmTrainingPlan);
    }

    @Override
    public int insertTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        tpmTrainingPlan.setCreateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.insertTpmTrainingPlan(tpmTrainingPlan);
    }

    @Override
    public int updateTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        tpmTrainingPlan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(tpmTrainingPlan);
    }

    @Override
    public int deleteTpmTrainingPlanByPlanId(Long planId)
    {
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanId(planId);
    }

    @Override
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds)
    {
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanIds(planIds);
    }
}
