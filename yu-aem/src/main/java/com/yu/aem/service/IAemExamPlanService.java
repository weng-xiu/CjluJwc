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

    /**
     * A1：考试自动编排。按考生名单贪心拆分到多个可用教室（跳过同时段已占用教室），
     * 超容量自动拆分，蛇形排座，编排完成后置为已安排。
     */
    public java.util.Map<String, Object> autoArrangeExam(Long examId);

    /**
     * A2：考试冲突检测。检测指定学期内时间重叠的考试两两之间的
     * 教室/学生/监考教师冲突，返回可视化冲突列表。
     */
    public java.util.List<java.util.Map<String, Object>> detectExamConflicts(Long semesterId);
}
