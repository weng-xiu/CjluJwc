package com.yu.web.service;

import java.util.List;
import java.util.Map;
import com.yu.web.domain.SubjectReportVo;

/**
 * 主题分析Service接口（P3）
 *
 * @author ruoyi
 * @date 2026-09-25
 */
public interface ISubjectStatService
{
    /** 学生结构主题：总览 + 院系/入学年份/性别/培养层次分布 */
    public Map<String, Object> selectStudentStructure();

    /** 成绩分析主题：总览 + 分数段分布 + 院系聚合 + 学期趋势（学期可选过滤） */
    public Map<String, Object> selectGradeAnalysis(Long semesterId);

    /** 师资分析主题：总览 + 院系/职称/学历/性别分布 + 授课工作量TOP（学期可选过滤） */
    public Map<String, Object> selectTeacherStructure(Long semesterId);

    /** 师生数据上报报表（分院系汇总，用于Excel导出） */
    public List<SubjectReportVo> selectReportByDept(Long semesterId);
}
