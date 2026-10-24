package com.yu.sam.exporter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.ProgramQualityExportVo;

/**
 * F3 专业质量档案导出器单测：验证「板块/项目/内容」摊平规则（纯函数）。
 *
 * @author F3
 */
class SamProgramQualityExporterTest
{
    private final SamProgramQualityExporter exporter = new SamProgramQualityExporter();

    private Map<String, Object> map(Object... kv)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2)
        {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    @Test
    @DisplayName("null 档案：输出说明行")
    void flatten_nullProfile()
    {
        List<ProgramQualityExportVo> rows = exporter.flatten(null);
        assertEquals(1, rows.size());
        assertEquals("无档案数据", rows.get(0).getValue());
    }

    @Test
    @DisplayName("不可用档案：输出 note")
    void flatten_notAvailable()
    {
        Map<String, Object> profile = map("available", false, "note", "未找到该专业基础信息");
        List<ProgramQualityExportVo> rows = exporter.flatten(profile);
        assertEquals(1, rows.size());
        assertEquals("未找到该专业基础信息", rows.get(0).getValue());
    }

    @Test
    @DisplayName("完整档案：各板块按序摊平且空值跳过")
    void flatten_fullProfile()
    {
        ObeAttainmentVo vo = new ObeAttainmentVo();
        vo.setGrCode("R1");
        vo.setGrName("工程知识");
        vo.setAttainment(0.82);
        vo.setTargetValue(0.7);
        vo.setAchieved("1");

        Map<String, Object> profile = map(
                "available", true,
                "major", map("majorCode", "080601", "majorName", "计算机科学与技术", "deptName", "信息工程部"),
                "studentStats", map("totalCount", 100L, "enrolledCount", 80L),
                "faculty", map("summary", map("teacherCount", 20L, "seniorCount", 8L),
                        "titleDist", List.of(map("title", "教授", "cnt", 3L)),
                        "educationDist", new ArrayList<>()),
                "plan", map("planId", 100L, "planName", "计算机2023版"),
                "creditStructure", List.of(map("creditTypeName", "专业核心", "requiredCredit", 60.0)),
                "courseStats", map("courseCount", 12L, "practiceHours", 300L, "totalHours", 1000L),
                "offeringStats", map("offeringCount", 30L),
                "obeAttainment", map("totalRequirements", 5L, "achievedRequirements", 4L,
                        "requirements", List.of(vo)),
                "evaluation", map("evalCount", 50L, "avgScore", 88.5),
                "supervision", map("stats", map("recordCount", 12L),
                        "levelDist", List.of(map("level", "3", "cnt", 1L))),
                "completeness", map("coveredSections", 7, "totalSections", 7));

        List<ProgramQualityExportVo> rows = exporter.flatten(profile);

        assertTrue(rows.stream().anyMatch(r -> "专业基础".equals(r.getSection()) && "计算机科学与技术".equals(r.getValue())));
        assertTrue(rows.stream().anyMatch(r -> "师资队伍".equals(r.getSection()) && "在岗教师数".equals(r.getItem()) && "20".equals(r.getValue())));
        assertTrue(rows.stream().anyMatch(r -> "师资队伍-职称".equals(r.getSection()) && "教授".equals(r.getItem())));
        assertTrue(rows.stream().anyMatch(r -> "课程教学".equals(r.getSection()) && "实践学时占比".equals(r.getItem()) && "30.0%".equals(r.getValue())));
        assertTrue(rows.stream().anyMatch(r -> "OBE达成度明细".equals(r.getSection()) && r.getValue().contains("已达成")));
        assertTrue(rows.stream().anyMatch(r -> "教学督导-等级".equals(r.getSection()) && "不合格".equals(r.getItem())));
        // 空 educationDist 不产生行
        assertTrue(rows.stream().noneMatch(r -> "师资队伍-学历".equals(r.getSection())));
    }
}
