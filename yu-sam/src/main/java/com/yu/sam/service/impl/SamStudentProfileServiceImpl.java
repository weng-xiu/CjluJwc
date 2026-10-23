package com.yu.sam.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.StudentProfileExportVo;
import com.yu.sam.exporter.SamStudentProfileExporter;
import com.yu.sam.mapper.SamStudentProfileMapper;
import com.yu.sam.service.ISamObeAttainmentService;
import com.yu.sam.service.ISamStudentProfileService;

/**
 * N6 学生数字档案全息画像服务实现（只读聚合，不写库）。
 *
 * @author N6
 */
@Service
public class SamStudentProfileServiceImpl implements ISamStudentProfileService
{
    private static final int RECENT_GRADE_LIMIT = 20;

    @Autowired
    private SamStudentProfileMapper profileMapper;

    @Autowired
    private ISamObeAttainmentService obeAttainmentService;

    @Autowired
    private SamStudentProfileExporter profileExporter;

    @Override
    public List<StudentProfileExportVo> buildProfileExport(Long studentId)
    {
        return profileExporter.flatten(buildProfile(studentId));
    }

    @Override
    public Map<String, Object> buildProfile(Long studentId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);

        Map<String, Object> base = studentId == null ? null : profileMapper.selectStudentBase(studentId);
        if (base == null || base.isEmpty())
        {
            result.put("available", false);
            result.put("note", "未找到该学生学籍信息，无法生成数字档案。");
            return result;
        }
        result.put("available", true);
        result.put("student", base);

        Map<String, Object> academic = profileMapper.selectAcademicSummary(studentId);
        result.put("academicSummary", academic);

        Map<String, Object> plan = profileMapper.selectPlanInfo(studentId);
        result.put("plan", plan);
        Long planId = plan == null ? null : asLong(plan.get("planId"));
        if (planId != null)
        {
            result.put("creditStructure", buildCreditStructure(planId, studentId));
        }
        else
        {
            result.put("creditStructure", new ArrayList<>());
        }

        // OBE 达成度（复用 N1，个体口径）
        List<ObeAttainmentVo> obe = planId == null ? new ArrayList<>()
                : obeAttainmentService.calculateByStudent(planId, studentId);
        result.put("obeAttainment", obe);

        List<Map<String, Object>> warnings = profileMapper.selectWarnings(studentId);
        result.put("warnings", warnings);

        result.put("reviews", profileMapper.selectReviewRecords(studentId));
        result.put("thesis", profileMapper.selectThesis(studentId));
        result.put("recentGrades", profileMapper.selectRecentGrades(studentId, RECENT_GRADE_LIMIT));

        result.put("completeness", buildCompleteness(result, warnings, obe));
        result.put("note", "数字档案由各业务模块只读聚合生成，各板块数据以来源系统当前状态为准。");
        return result;
    }

    /** 学分结构达成：方案分项要求 vs 学生按课程属性已获学分。 */
    private List<Map<String, Object>> buildCreditStructure(Long planId, Long studentId)
    {
        List<Map<String, Object>> sections = profileMapper.selectCreditSections(planId);
        if (sections == null || sections.isEmpty())
        {
            return new ArrayList<>();
        }
        Map<String, Double> earnedByCat = new HashMap<>();
        for (Map<String, Object> e : profileMapper.selectEarnedByCategory(studentId))
        {
            Object ct = e.get("creditType");
            if (ct != null)
            {
                earnedByCat.put(ct.toString(), asDouble(e.get("earnedCredit")));
            }
        }
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> s : sections)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            String creditType = s.get("creditType") == null ? null : s.get("creditType").toString();
            double required = asDouble(s.get("requiredCredit"));
            double got = creditType == null ? 0d : earnedByCat.getOrDefault(creditType, 0d);
            row.put("creditType", creditType);
            row.put("creditTypeName", s.get("creditTypeName"));
            row.put("requiredCredit", required);
            row.put("earnedCredit", round2(got));
            row.put("rate", required > 0 ? round1(Math.min(got / required, 1d) * 100) : null);
            merged.add(row);
        }
        return merged;
    }

    /** 各板块完备度：标记是否有数据，并给出覆盖计数。 */
    private Map<String, Object> buildCompleteness(Map<String, Object> result,
                                                  List<Map<String, Object>> warnings,
                                                  List<ObeAttainmentVo> obe)
    {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("student", true);
        c.put("academicSummary", hasData(result.get("academicSummary"), "courseCount"));
        c.put("plan", result.get("plan") instanceof Map && !((Map<?, ?>) result.get("plan")).isEmpty());
        c.put("creditStructure", !((List<?>) result.get("creditStructure")).isEmpty());
        c.put("obeAttainment", obe != null && !obe.isEmpty());
        c.put("warnings", warnings != null && !warnings.isEmpty());
        c.put("reviews", !((List<?>) result.get("reviews")).isEmpty());
        c.put("thesis", result.get("thesis") instanceof Map && !((Map<?, ?>) result.get("thesis")).isEmpty());
        c.put("recentGrades", !((List<?>) result.get("recentGrades")).isEmpty());
        int covered = 0;
        for (Object v : c.values())
        {
            if (Boolean.TRUE.equals(v))
            {
                covered++;
            }
        }
        c.put("coveredSections", covered);
        c.put("totalSections", 9);
        return c;
    }

    private boolean hasData(Object section, String probeKey)
    {
        return section instanceof Map && !((Map<?, ?>) section).isEmpty()
                && ((Map<?, ?>) section).get(probeKey) != null;
    }

    private static Long asLong(Object o)
    {
        return o instanceof Number ? ((Number) o).longValue() : null;
    }

    private static double asDouble(Object o)
    {
        return o instanceof Number ? ((Number) o).doubleValue() : 0d;
    }

    private static double round2(double v)
    {
        return Math.round(v * 100d) / 100d;
    }

    private static double round1(double v)
    {
        return Math.round(v * 10d) / 10d;
    }
}
