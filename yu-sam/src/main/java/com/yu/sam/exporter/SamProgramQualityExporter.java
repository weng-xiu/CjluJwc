package com.yu.sam.exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.ProgramQualityExportVo;

/**
 * 专业质量档案一页纸扁平化导出器（F3 档案导出）。
 *
 * 将 buildProfile 的嵌套结构摊平为「板块 / 项目 / 内容」三列，
 * 与 ExcelUtil 单 Sheet 导出兼容；纯函数、无外部依赖，便于单测。
 *
 * @author F3
 */
@Component
public class SamProgramQualityExporter
{
    /**
     * 将专业质量档案摊平为导出行。
     *
     * @param profile buildProfile 返回的档案
     * @return 导出行（板块/项目/内容）
     */
    @SuppressWarnings("unchecked")
    public List<ProgramQualityExportVo> flatten(Map<String, Object> profile)
    {
        List<ProgramQualityExportVo> rows = new ArrayList<>();
        if (profile == null)
        {
            rows.add(new ProgramQualityExportVo("档案", "说明", "无档案数据"));
            return rows;
        }
        if (Boolean.FALSE.equals(profile.get("available")))
        {
            rows.add(new ProgramQualityExportVo("档案", "说明", text(profile.get("note"), "未找到该专业基础信息")));
            return rows;
        }

        Map<String, Object> major = asMap(profile.get("major"));
        put(rows, "专业基础", "专业编码", major.get("majorCode"));
        put(rows, "专业基础", "专业名称", major.get("majorName"));
        put(rows, "专业基础", "所属院系", major.get("deptName"));
        put(rows, "专业基础", "培养层次", major.get("educationLevel"));
        put(rows, "专业基础", "学制(年)", major.get("duration"));

        Map<String, Object> students = asMap(profile.get("studentStats"));
        put(rows, "培养规模", "学籍总数", students.get("totalCount"));
        put(rows, "培养规模", "在读人数", students.get("enrolledCount"));
        put(rows, "培养规模", "毕业人数", students.get("graduatedCount"));

        Map<String, Object> faculty = asMap(profile.get("faculty"));
        Map<String, Object> fs = asMap(faculty.get("summary"));
        put(rows, "师资队伍", "在岗教师数", fs.get("teacherCount"));
        put(rows, "师资队伍", "高级职称数", fs.get("seniorCount"));
        put(rows, "师资队伍", "博士教师数", fs.get("doctorCount"));
        flattenDist(rows, "师资队伍-职称", faculty.get("titleDist"), "title");
        flattenDist(rows, "师资队伍-学历", faculty.get("educationDist"), "education");

        Map<String, Object> plan = asMap(profile.get("plan"));
        put(rows, "培养方案", "方案名称", plan.get("planName"));
        put(rows, "培养方案", "方案年度", plan.get("planYear"));
        put(rows, "培养方案", "版本", plan.get("version"));
        put(rows, "培养方案", "总学分", plan.get("totalCredits"));
        put(rows, "培养方案", "已发布方案数", profile.get("plans") instanceof List ? ((List<Object>) profile.get("plans")).size() : null);

        Map<String, Object> course = asMap(profile.get("courseStats"));
        put(rows, "课程教学", "方案课程门数", course.get("courseCount"));
        put(rows, "课程教学", "课程总学分", course.get("totalCredit"));
        put(rows, "课程教学", "实践环节门数", course.get("practiceCount"));
        put(rows, "课程教学", "实践环节学分", course.get("practiceCredit"));
        put(rows, "课程教学", "实践学时占比", hourRatio(course.get("practiceHours"), course.get("totalHours")));
        Map<String, Object> offering = asMap(profile.get("offeringStats"));
        put(rows, "课程教学", "累计开课批次", offering.get("offeringCount"));
        put(rows, "课程教学", "累计教学班数", offering.get("classCount"));

        Object creditStructure = profile.get("creditStructure");
        if (creditStructure instanceof List)
        {
            for (Object item : (List<Object>) creditStructure)
            {
                Map<String, Object> c = asMap(item);
                String type = text(c.get("creditTypeName"), text(c.get("creditType"), "未分类"));
                put(rows, "学分结构", type, "要求学分 " + text(c.get("requiredCredit"), "—"));
            }
        }

        Object obe = profile.get("obeAttainment");
        if (obe instanceof Map && !((Map<?, ?>) obe).isEmpty())
        {
            Map<String, Object> board = asMap(obe);
            put(rows, "OBE达成度", "毕业要求总数", board.get("totalRequirements"));
            put(rows, "OBE达成度", "已达成数", board.get("achievedRequirements"));
            put(rows, "OBE达成度", "达成率", board.get("achievedRate"));
            put(rows, "OBE达成度", "平均达成度", board.get("averageAttainment"));
            Object requirements = board.get("requirements");
            if (requirements instanceof List)
            {
                for (Object item : (List<Object>) requirements)
                {
                    if (item instanceof ObeAttainmentVo)
                    {
                        ObeAttainmentVo vo = (ObeAttainmentVo) item;
                        String itemName = (vo.getGrCode() == null ? "" : vo.getGrCode() + " ") +
                                text(vo.getGrName(), "毕业要求");
                        put(rows, "OBE达成度明细", itemName,
                                "达成度 " + text(vo.getAttainment(), "—") + " / 目标 " + text(vo.getTargetValue(), "—")
                                        + " / " + ("1".equals(vo.getAchieved()) ? "已达成" : "未达成"));
                    }
                }
            }
        }

        Map<String, Object> evaluation = asMap(profile.get("evaluation"));
        put(rows, "学生评教", "评教份数", evaluation.get("evalCount"));
        put(rows, "学生评教", "平均分", evaluation.get("avgScore"));
        put(rows, "学生评教", "优秀份数(≥90)", evaluation.get("excellentCount"));

        Map<String, Object> supervision = asMap(profile.get("supervision"));
        Map<String, Object> ss = asMap(supervision.get("stats"));
        put(rows, "教学督导", "听课记录数", ss.get("recordCount"));
        put(rows, "教学督导", "平均评分", ss.get("avgScore"));
        put(rows, "教学督导", "不合格次数", ss.get("unqualifiedCount"));
        flattenDist(rows, "教学督导-等级", supervision.get("levelDist"), "level");
        // 等级码翻译见 levelLabel（0优秀 1良好 2合格 3不合格）

        Map<String, Object> completeness = asMap(profile.get("completeness"));
        put(rows, "数据完备度", "已覆盖板块", completeness.get("coveredSections"));
        put(rows, "数据完备度", "板块总数", completeness.get("totalSections"));

        if (rows.isEmpty())
        {
            rows.add(new ProgramQualityExportVo("档案", "说明", "档案各板块均无数据"));
        }
        return rows;
    }

    private void flattenDist(List<ProgramQualityExportVo> rows, String section, Object dist, String key)
    {
        if (!(dist instanceof List))
        {
            return;
        }
        for (Object item : (List<?>) dist)
        {
            Map<String, Object> d = asMap(item);
            Object raw = d.get(key);
            String name = "level".equals(key) ? levelLabel(raw) : text(raw, "未分类");
            put(rows, section, name, "人数 " + text(d.get("cnt"), "—"));
        }
    }

    /** 督导评价等级码翻译（0优秀 1良好 2合格 3不合格）。 */
    private String levelLabel(Object value)
    {
        if ("0".equals(value)) return "优秀";
        if ("1".equals(value)) return "良好";
        if ("2".equals(value)) return "合格";
        if ("3".equals(value)) return "不合格";
        return text(value, "其他");
    }

    /** 实践学时占比：practice/total 百分比文本。 */
    private String hourRatio(Object practice, Object total)
    {
        double p = toDouble(practice);
        double t = toDouble(total);
        if (t <= 0)
        {
            return null;
        }
        return String.format("%.1f%%", p * 100.0 / t);
    }

    private double toDouble(Object v)
    {
        return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
    }

    private void put(List<ProgramQualityExportVo> rows, String section, String item, Object value)
    {
        String v = text(value, null);
        if (v == null || v.trim().isEmpty() || "—".equals(v))
        {
            return;
        }
        rows.add(new ProgramQualityExportVo(section, item, v));
    }

    private Map<String, Object> asMap(Object value)
    {
        if (value instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return Collections.emptyMap();
    }

    private String text(Object value, String fallback)
    {
        if (value == null)
        {
            return fallback;
        }
        String s = value instanceof Double
                ? java.math.BigDecimal.valueOf((Double) value).stripTrailingZeros().toPlainString()
                : String.valueOf(value);
        return s.isEmpty() ? fallback : s;
    }
}
