package com.yu.tpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * T3：统计同专业同学年已发布方案数（排除自身，用于发布冲突校验）
     *
     * @param majorId       专业ID
     * @param planYear      方案年份
     * @param excludePlanId 需排除的方案ID（可为null）
     * @return 已发布方案数
     */
    public int countPublishedConflict(@Param("majorId") Long majorId, @Param("planYear") String planYear, @Param("excludePlanId") Long excludePlanId);
}

