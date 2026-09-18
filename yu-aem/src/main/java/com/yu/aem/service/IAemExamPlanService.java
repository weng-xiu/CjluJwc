package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemExamPlan;

/**
 * 考试安排Service接口
 *
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemExamPlanService
{
    public AemExamPlan selectAemExamPlanByExamId(Long examId);

    /** 查询考试安排明细（含座位、监考子表） */
    public AemExamPlan selectAemExamPlanDetail(Long examId);

    public List<AemExamPlan> selectAemExamPlanList(AemExamPlan aemExamPlan);
    /** 门户端查询：不套用部门数据范围（访问已由 portal:exam:list 权限控制） */
    public List<AemExamPlan> selectAemExamPlanListForPortal(AemExamPlan aemExamPlan);
    public int insertAemExamPlan(AemExamPlan aemExamPlan);
    public int updateAemExamPlan(AemExamPlan aemExamPlan);
    public int deleteAemExamPlanByExamIds(Long[] examIds);
    public int deleteAemExamPlanByExamId(Long examId);
}
