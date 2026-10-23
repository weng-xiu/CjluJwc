package com.yu.sam.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.StudentProfileExportVo;

/**
 * 学生数字档案扁平化导出器（F4 档案导出）。
 *
 * 将 buildProfile 的嵌套结构摊平为「板块 / 项目 / 内容」三列，
 * 与 ExcelUtil 单 Sheet 导出兼容；纯函数、无外部依赖，便于单测。
 *
 * @author F4
 */
@Component
public class SamStudentProfileExporter
{
    /**
     * 将全息档案摊平为导出行。
     *
     * @param profile buildProfile 返回的档案
     * @return 导出行（板块/项目/内容）
     */
    @SuppressWarnings("unchecked")
    public List<StudentProfileExportVo> flatten(Map<String, Object> profile)
    {
        List<StudentProfileExportVo> rows = new ArrayList<>();
        if (profile == null)
        {
            rows.add(new StudentProfileExportVo("档案", "说明", "无档案数据"));
            return rows;
        }
        if (Boolean.FALSE.equals(profile.get("available")))
        {
            rows.add(new StudentProfileExportVo("档案", "说明", text(profile.get("note"), "未找到该学生学籍信息")));
            return rows;
        }

        Map<String, Object> student = asMap(profile.get("student"));
        put(rows, "学籍基础", "学号", student.get("studentNo"));
        put(rows, "学籍基础", "姓名", student.get("studentName"));
        put(rows, "学籍基础", "性别", gender(student.get("gender")));
        put(rows, "学籍基础", "入学年份", student.get("enrollmentYear"));
        put(rows, "学籍基础", "院系", student.get("deptName"));
        put(rows, "学籍基础", "专业", student.get("majorName"));
        put(rows, "学籍基础", "班级", student.get("className"));
        put(rows, "学籍基础", "培养层次", student.get("educationLevel"));

        Map<String, Object> academic = asMap(profile.get("academicSummary"));
        put(rows, "学业汇总", "修读门数", academic.get("courseCount"));
        put(rows, "学业汇总", "已获学分", academic.get("earnedCredit"));
        put(rows, "学业汇总", "已修学分", academic.get("attemptedCredit"));
        put(rows, "学业汇总", "GPA", academic.get("gpa"));
        put(rows, "学业汇总", "加权均分", academic.get("avgScore"));
        put(rows, "学业汇总", "不及格门数", academic.get("failCount"));

        Map<String, Object> plan = asMap(profile.get("plan"));
        put(rows, "培养方案", "方案名称", plan.get("planName"));
        put(rows, "培养方案", "方案年度", plan.get("planYear"));
        put(rows, "培养方案", "版本", plan.get("version"));
        put(rows, "培养方案", "总学分", plan.get("totalCredits"));

        Map<String, Object> completeness = asMap(profile.get("completeness"));
        put(rows, "数据完备度", "已覆盖板块", completeness.get("coveredSections"));
        put(rows, "数据完备度", "板块总数", completeness.get("totalSections"));

        Object creditStructure = profile.get("creditStructure");
        if (creditStructure instanceof List)
        {
            for (Object item : (List<Object>) creditStructure)
            {
                Map<String, Object> c = asMap(item);
                String type = text(c.get("creditTypeName"), text(c.get("creditType"), "未分类"));
                put(rows, "学分结构", type,
                        "要求 " + text(c.get("requiredCredit"), "—") + " / 已获 " + text(c.get("earnedCredit"), "—")
                                + " / 达成率 " + text(c.get("rate"), "—") + "%");
            }
        }

        Object obeList = profile.get("obeAttainment");
        if (obeList instanceof List)
        {
            for (Object item : (List<Object>) obeList)
            {
                if (item instanceof ObeAttainmentVo)
                {
                    ObeAttainmentVo vo = (ObeAttainmentVo) item;
                    String itemName = (vo.getGrCode() == null ? "" : vo.getGrCode() + " ") +
                            text(vo.getGrName(), "毕业要求");
                    put(rows, "OBE达成度", itemName,
                            "达成度 " + text(vo.getAttainment(), "—") + " / 目标 " + text(vo.getTargetValue(), "—")
                                    + " / " + ("1".equals(vo.getAchieved()) ? "已达成" : "未达成"));
                }
            }
        }

        Object warnings = profile.get("warnings");
        if (warnings instanceof List)
        {
            for (Object item : (List<Object>) warnings)
            {
                Map<String, Object> w = asMap(item);
                put(rows, "学业预警", text(w.get("warningDate"), "预警记录"),
                        "类型 " + text(w.get("warningType"), "—") + " / 级别 " + text(w.get("warningLevel"), "—")
                                + " / 原因 " + text(w.get("warningReason"), "—")
                                + " / " + ("1".equals(w.get("isResolved")) ? "已解除" : "未解除"));
            }
        }

        Object grades = profile.get("recentGrades");
        if (grades instanceof List)
        {
            for (Object item : (List<Object>) grades)
            {
                Map<String, Object> g = asMap(item);
                put(rows, "近期成绩", text(g.get("courseName"), "课程"),
                        "学分 " + text(g.get("credit"), "—") + " / 总评 " + text(g.get("totalScore"), "—")
                                + " / 绩点 " + text(g.get("gradePoint"), "—")
                                + " / " + ("1".equals(g.get("isPass")) ? "通过" : "未通过"));
            }
        }

        if (rows.isEmpty())
        {
            rows.add(new StudentProfileExportVo("档案", "说明", "档案各板块均无数据"));
        }
        return rows;
    }

    private void put(List<StudentProfileExportVo> rows, String section, String item, Object value)
    {
        String v = text(value, null);
        if (v == null || v.trim().isEmpty() || "—".equals(v))
        {
            return;
        }
        rows.add(new StudentProfileExportVo(section, item, v));
    }

    private Map<String, Object> asMap(Object value)
    {
        if (value instanceof Map)
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return map;
        }
        return java.util.Collections.emptyMap();
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

    private String gender(Object value)
    {
        if ("0".equals(value))
        {
            return "男";
        }
        if ("1".equals(value))
        {
            return "女";
        }
        return value == null ? null : String.valueOf(value);
    }
}
