package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmTrainingPlan;

/**
 * 人才培养方案Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmTrainingPlanMapper 
{
    public TpmTrainingPlan selectTpmTrainingPlanByPlanId(Long planId);
    public List<TpmTrainingPlan> selectTpmTrainingPlanList(TpmTrainingPlan tpmTrainingPlan);
    public int insertTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan);
    public int updateTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan);
    public int deleteTpmTrainingPlanByPlanId(Long planId);
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds);
}

