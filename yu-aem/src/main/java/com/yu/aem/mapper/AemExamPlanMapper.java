package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemExamPlan;

/**
 * 考试安排Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemExamPlanMapper 
{
    public AemExamPlan selectAemExamPlanByExamId(Long examId);
    public List<AemExamPlan> selectAemExamPlanList(AemExamPlan aemExamPlan);
    public int insertAemExamPlan(AemExamPlan aemExamPlan);
    public int updateAemExamPlan(AemExamPlan aemExamPlan);
    public int deleteAemExamPlanByExamId(Long examId);
    public int deleteAemExamPlanByExamIds(Long[] examIds);
}
