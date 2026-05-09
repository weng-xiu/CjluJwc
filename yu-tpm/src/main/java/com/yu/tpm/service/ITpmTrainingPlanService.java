package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmTrainingPlan;

/**
 * 人才培养方案Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmTrainingPlanService 
{
    public TpmTrainingPlan selectTpmTrainingPlanByPlanId(Long planId);
    public List<TpmTrainingPlan> selectTpmTrainingPlanList(TpmTrainingPlan tpmTrainingPlan);
    public int insertTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan);
    public int updateTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan);
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds);
    public int deleteTpmTrainingPlanByPlanId(Long planId);
}
