package com.yu.sam.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.StudentProfileExportVo;

/**
 * 学生数字档案扁平化导出器单测（F4）。
 * 纯函数校验：空档案兜底、学籍/学业/学分结构/OBE/预警/成绩落行、空值跳过、性别转义。
 *
 * @author F4
 */
class SamStudentProfileExporterTest
{
    private final SamStudentProfileExporter exporter = new SamStudentProfileExporter();

    @Test
    @DisplayName("flatten：null 档案返回说明行")
    void flatten_nullProfile()
    {
        List<StudentProfileExportVo> rows = exporter.flatten(null);
        assertEquals(1, rows.size());
        assertEquals("档案", rows.get(0).getSection());
        assertEquals("无档案数据", rows.get(0).getValue());
    }

    @Test
    @DisplayName("flatten：学籍不存在时保留提示文案")
    void flatten_unavailable()
    {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("available", false);
        profile.put("note", "未找到该学生学籍信息，无法生成数字档案。");

        List<StudentProfileExportVo> rows = exporter.flatten(profile);
        assertEquals(1, rows.size());
        assertEquals("未找到该学生学籍信息，无法生成数字档案。", rows.get(0).getValue());
    }

    @Test
    @DisplayName("flatten：完整档案各板块落行且空值跳过")
    void flatten_fullProfile()
    {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("available", true);

        Map<String, Object> student = new LinkedHashMap<>();
        student.put("studentNo", "2024001");
        student.put("studentName", "李同学");
        student.put("gender", "0");
        student.put("enrollmentYear", "2024");
        student.put("deptName", null);
        profile.put("student", student);

        Map<String, Object> academic = new LinkedHashMap<>();
        academic.put("courseCount", 32);
        academic.put("gpa", 2.75d);
        academic.put("failCount", 3);
        profile.put("academicSummary", academic);

        Map<String, Object> completeness = new LinkedHashMap<>();
        completeness.put("coveredSections", 7);
        completeness.put("totalSections", 9);
        profile.put("completeness", completeness);

        List<Map<String, Object>> creditStructure = new ArrayList<>();
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("creditTypeName", "通识必修");
        section.put("requiredCredit", 40);
        section.put("earnedCredit", 36);
        section.put("rate", 90);
        creditStructure.add(section);
        profile.put("creditStructure", creditStructure);

        List<ObeAttainmentVo> obe = new ArrayList<>();
        ObeAttainmentVo vo = new ObeAttainmentVo();
        vo.setGrCode("GR1");
        vo.setGrName("工程知识");
        vo.setAttainment(0.82d);
        vo.setTargetValue(0.75d);
        vo.setAchieved("1");
        obe.add(vo);
        profile.put("obeAttainment", obe);

        List<Map<String, Object>> warnings = new ArrayList<>();
        Map<String, Object> warning = new LinkedHashMap<>();
        warning.put("warningDate", "2026-03-01");
        warning.put("warningType", "成绩预警");
        warning.put("isResolved", "0");
        warnings.add(warning);
        profile.put("warnings", warnings);

        List<Map<String, Object>> grades = new ArrayList<>();
        Map<String, Object> grade = new LinkedHashMap<>();
        grade.put("courseName", "高等数学");
        grade.put("credit", 5);
        grade.put("totalScore", 88.5d);
        grade.put("isPass", "1");
        grades.add(grade);
        profile.put("recentGrades", grades);

        List<StudentProfileExportVo> rows = exporter.flatten(profile);

        assertTrue(contains(rows, "学籍基础", "学号", "2024001"));
        assertTrue(contains(rows, "学籍基础", "性别", "男"));
        assertFalse(containsItem(rows, "院系"));
        assertTrue(contains(rows, "学业汇总", "GPA", "2.75"));
        assertTrue(contains(rows, "数据完备度", "板块总数", "9"));
        assertTrue(containsItem(rows, "通识必修"));
        assertTrue(contains(rows, "OBE达成度", "GR1 工程知识", "达成度 0.82 / 目标 0.75 / 已达成"));
        assertTrue(containsItem(rows, "2026-03-01"));
        assertTrue(containsItem(rows, "高等数学"));
    }

    @Test
    @DisplayName("flatten：各板块均无数据时给出兜底行")
    void flatten_emptySections()
    {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("available", true);

        List<StudentProfileExportVo> rows = exporter.flatten(profile);
        assertEquals(1, rows.size());
        assertEquals("档案各板块均无数据", rows.get(0).getValue());
    }

    private boolean contains(List<StudentProfileExportVo> rows, String section, String item, String value)
    {
        for (StudentProfileExportVo row : rows)
        {
            if (section.equals(row.getSection()) && item.equals(row.getItem()) && value.equals(row.getValue()))
            {
                return true;
            }
        }
        return false;
    }

    private boolean containsItem(List<StudentProfileExportVo> rows, String item)
    {
        for (StudentProfileExportVo row : rows)
        {
            if (item.equals(row.getItem()))
            {
                return true;
            }
        }
        return false;
    }
}
