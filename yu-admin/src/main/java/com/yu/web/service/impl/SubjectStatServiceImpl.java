package com.yu.web.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.web.domain.SubjectReportVo;
import com.yu.web.mapper.SubjectStatMapper;
import com.yu.web.service.ISubjectStatService;

/**
 * 主题分析Service实现（P3）
 *
 * 学生结构 / 成绩分析 / 师资分析三大主题 + 师生数据上报导出，
 * 全部指标来自真实业务表只读聚合。学期参数可选：为空时成绩/师资
 * 相关口径统计全部学期，避免早期学期数据缺失导致看板空白。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
@Service
public class SubjectStatServiceImpl implements ISubjectStatService
{
    @Autowired
    private SubjectStatMapper subjectStatMapper;

    @Override
    public Map<String, Object> selectStudentStructure()
    {
        Map<String, Object> result = new HashMap<>();
        result.put("semester", subjectStatMapper.selectCurrentSemester());
        result.put("overview", subjectStatMapper.selectStudentOverview());
        result.put("byDept", subjectStatMapper.selectStudentByDept());
        result.put("byEnrollmentYear", subjectStatMapper.selectStudentByEnrollmentYear());
        result.put("byGender", subjectStatMapper.selectStudentByGender());
        result.put("byEducationLevel", subjectStatMapper.selectStudentByEducationLevel());
        return result;
    }

    @Override
    public Map<String, Object> selectGradeAnalysis(Long semesterId)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("semester", subjectStatMapper.selectCurrentSemester());
        result.put("overview", subjectStatMapper.selectGradeOverview(semesterId));
        result.put("scoreBand", subjectStatMapper.selectGradeScoreBand(semesterId));
        result.put("byDept", subjectStatMapper.selectGradeByDept(semesterId));
        result.put("trend", subjectStatMapper.selectGradeTrend());
        return result;
    }

    @Override
    public Map<String, Object> selectTeacherStructure(Long semesterId)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("semester", subjectStatMapper.selectCurrentSemester());
        result.put("overview", subjectStatMapper.selectTeacherOverview(semesterId));
        result.put("byDept", subjectStatMapper.selectTeacherByDept());
        result.put("byTitle", subjectStatMapper.selectTeacherByTitle());
        result.put("byEducation", subjectStatMapper.selectTeacherByEducation());
        result.put("byGender", subjectStatMapper.selectTeacherByGender());
        result.put("workloadTop", subjectStatMapper.selectTeacherWorkloadTop(semesterId, 10));
        return result;
    }

    @Override
    public List<SubjectReportVo> selectReportByDept(Long semesterId)
    {
        List<Map<String, Object>> rows = subjectStatMapper.selectReportByDept(semesterId);
        List<SubjectReportVo> list = new ArrayList<>();
        if (rows != null)
        {
            for (Map<String, Object> row : rows)
            {
                SubjectReportVo vo = new SubjectReportVo();
                vo.setDeptName(toStr(row.get("deptName")));
                vo.setTotalStudent(toLong(row.get("totalStudent")));
                vo.setEnrolledStudent(toLong(row.get("enrolledStudent")));
                vo.setTeacherCount(toLong(row.get("teacherCount")));
                vo.setOfferingCount(toLong(row.get("offeringCount")));
                vo.setAvgPassRate(toStr(row.get("avgPassRate")));
                list.add(vo);
            }
        }
        return list;
    }

    /** 安全转字符串（null → 空串） */
    private String toStr(Object o)
    {
        return o == null ? "" : String.valueOf(o);
    }

    /** 安全转 Long（兼容 MySQL SUM 返回的 BigDecimal / Long / Integer，null → 0） */
    private Long toLong(Object o)
    {
        if (o == null)
        {
            return 0L;
        }
        if (o instanceof Number)
        {
            return ((Number) o).longValue();
        }
        try
        {
            return new BigDecimal(String.valueOf(o)).longValue();
        }
        catch (NumberFormatException e)
        {
            return 0L;
        }
    }
}
