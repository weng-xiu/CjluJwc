package com.yu.aem.service;

import java.util.List;
import java.util.Map;

/**
 * 评教统计Service接口（A6）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface IAemEvaluationStatService
{
    /** 总览 + 分数段分布 */
    public Map<String, Object> overview(Long questionnaireId, Long semesterId);

    /** 按课程聚合 */
    public List<Map<String, Object>> byCourse(Long questionnaireId, Long semesterId);

    /** 按教师聚合（附带排名） */
    public List<Map<String, Object>> byTeacher(Long questionnaireId, Long semesterId);

    /** 按班级聚合 */
    public List<Map<String, Object>> byClass(Long questionnaireId, Long semesterId, Long courseId);

    /** 月度趋势 */
    public List<Map<String, Object>> trend(Long questionnaireId);

    /** 教师个人反馈分析报告（汇总 + 分课程 + 分布 + 评语词频） */
    public Map<String, Object> teacherReport(Long teacherId);

    /** 某教师按课程拆分明细（真实课程名+学期名），供门户教师结果列表 */
    public List<Map<String, Object>> teacherCourseBreakdown(Long teacherId);

    /** 评语词频分析 */
    public Map<String, Object> commentAnalysis(Long teacherId, Long courseId, Long questionnaireId);
}
