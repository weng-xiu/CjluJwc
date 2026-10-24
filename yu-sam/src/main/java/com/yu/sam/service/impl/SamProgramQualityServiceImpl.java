package com.yu.sam.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.sam.exporter.SamProgramQualityExporter;
import com.yu.sam.domain.dto.ProgramQualityExportVo;
import com.yu.sam.mapper.SamProgramQualityMapper;
import com.yu.sam.service.ISamObeAttainmentService;
import com.yu.sam.service.ISamProgramQualityService;

/**
 * F3 专业质量档案一页纸 Service 实现（跨模块只读聚合，镜像 N6 学生数字档案范式）。
 *
 * <p>以专业为主线，只读聚合 brm（师资）、tpm（方案/课程/学分结构/开课）、
 * aem（评教/督导）、sam（学籍规模、OBE 达成度复用 N1 群体口径）各模块质量指标，
 * 不写库、不引入新表。
 *
 * @author F3
 */
@Service
public class SamProgramQualityServiceImpl implements ISamProgramQualityService
{
    @Autowired
    private SamProgramQualityMapper programQualityMapper;

    @Autowired
    private ISamObeAttainmentService obeAttainmentService;

    @Autowired
    private SamProgramQualityExporter profileExporter;

    @Override
    public List<Map<String, Object>> listMajors()
    {
        return programQualityMapper.selectMajorOptions();
    }

    @Override
    public Map<String, Object> buildProfile(Long majorId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("majorId", majorId);

        Map<String, Object> major = majorId == null ? null : programQualityMapper.selectMajorBase(majorId);
        if (major == null || major.isEmpty())
        {
            result.put("available", false);
            result.put("note", "未找到该专业基础信息，无法生成质量档案。");
            return result;
        }
        result.put("available", true);
        result.put("major", major);

        // 培养规模
        result.put("studentStats", programQualityMapper.selectStudentStats(majorId));

        // 师资队伍（专业所属院系口径）
        Long deptId = asLong(major.get("deptId"));
        Map<String, Object> faculty = new LinkedHashMap<>();
        if (deptId != null)
        {
            faculty.put("summary", programQualityMapper.selectFacultySummary(deptId));
            faculty.put("titleDist", programQualityMapper.selectTitleDist(deptId));
            faculty.put("educationDist", programQualityMapper.selectEducationDist(deptId));
        }
        result.put("faculty", faculty);

        // 培养方案（取最新已发布方案作为课程/学分/OBE 口径）
        List<Map<String, Object>> plans = programQualityMapper.selectPublishedPlans(majorId);
        result.put("plans", plans);
        Map<String, Object> plan = (plans == null || plans.isEmpty()) ? null : plans.get(0);
        result.put("plan", plan);
        Long planId = plan == null ? null : asLong(plan.get("planId"));
        if (planId != null)
        {
            result.put("creditStructure", programQualityMapper.selectCreditSections(planId));
            result.put("courseStats", programQualityMapper.selectCourseStats(planId));
            result.put("offeringStats", programQualityMapper.selectOfferingStats(planId));
            // OBE 达成度（复用 N1，群体口径）
            result.put("obeAttainment", obeAttainmentService.dashboard(planId, null));
        }
        else
        {
            result.put("creditStructure", new ArrayList<>());
            result.put("obeAttainment", null);
        }

        // 评教与督导（院系教师口径）
        if (deptId != null)
        {
            result.put("evaluation", programQualityMapper.selectEvalStats(deptId));
            Map<String, Object> supervision = new LinkedHashMap<>();
            supervision.put("stats", programQualityMapper.selectSupervisionStats(deptId));
            supervision.put("levelDist", programQualityMapper.selectSupervisionLevelDist(deptId));
            result.put("supervision", supervision);
        }

        result.put("completeness", buildCompleteness(result));
        result.put("note", "专业质量档案由各业务模块只读聚合生成：师资/评教/督导按专业所属院系口径，"
                + "课程/学分/OBE 按最新已发布培养方案口径，各板块数据以来源系统当前状态为准。");
        return result;
    }

    @Override
    public List<ProgramQualityExportVo> buildProfileExport(Long majorId)
    {
        return profileExporter.flatten(buildProfile(majorId));
    }

    /** 板块覆盖度：有数据的板块数 / 板块总数。 */
    private Map<String, Object> buildCompleteness(Map<String, Object> result)
    {
        String[] sections = {"studentStats", "faculty", "plan", "creditStructure", "obeAttainment", "evaluation", "supervision"};
        int covered = 0;
        for (String s : sections)
        {
            Object v = result.get(s);
            if (v instanceof Map && !((Map<?, ?>) v).isEmpty())
            {
                covered++;
            }
            else if (v instanceof List && !((List<?>) v).isEmpty())
            {
                covered++;
            }
        }
        Map<String, Object> completeness = new LinkedHashMap<>();
        completeness.put("coveredSections", covered);
        completeness.put("totalSections", sections.length);
        return completeness;
    }

    private Long asLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        try
        {
            return Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
