package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCreditStructure;

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

    /**
     * 发布培养方案（T3：含发布冲突校验与版本号补齐）
     */
    public int publishTrainingPlan(Long planId);

    /**
     * 废止培养方案
     */
    public int deprecateTrainingPlan(Long planId);

    /**
     * T3：复制培养方案（含课程与学分结构子表）为新的草稿版本，版本号自动递增
     *
     * @param planId 源方案ID
     * @return 新方案ID
     */
    public Long copyTrainingPlan(Long planId);

    /**
     * 保存培养方案主表及其子表（课程库、学分结构），仅做新增/修改，不删除子表
     */
    public int savePlanWithChildren(TpmTrainingPlan plan, List<TpmCourseLibrary> courseList, List<TpmCreditStructure> creditList);
}
