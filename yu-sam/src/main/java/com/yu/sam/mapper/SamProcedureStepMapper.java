package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamProcedureStep;

/**
 * 离校环节配置Mapper接口（S7）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public interface SamProcedureStepMapper
{
    public SamProcedureStep selectSamProcedureStepByStepId(Long stepId);

    public List<SamProcedureStep> selectSamProcedureStepList(SamProcedureStep samProcedureStep);

    /** 启用的环节清单（按排序） */
    public List<SamProcedureStep> selectActiveSteps();

    public int insertSamProcedureStep(SamProcedureStep samProcedureStep);

    public int updateSamProcedureStep(SamProcedureStep samProcedureStep);

    public int deleteSamProcedureStepByStepIds(Long[] stepIds);

    /** 环节编码唯一性校验（excludeStepId 用于修改时排除自身） */
    public int countByStepKey(@Param("stepKey") String stepKey, @Param("excludeStepId") Long excludeStepId);

    /** 各环节完成情况统计：stepId/stepKey/stepName/requiredFlag/total/doneCount */
    public List<Map<String, Object>> statProcedureSteps();
}
