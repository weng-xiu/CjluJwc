package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.sam.mapper.SamProgramQualityMapper;
import com.yu.sam.service.ISamObeAttainmentService;

/**
 * F3 专业质量档案一页纸服务单测（只 mock 聚合 Mapper 与 OBE 服务）。
 *
 * @author F3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamProgramQualityServiceImplTest
{
    @Mock
    private SamProgramQualityMapper programQualityMapper;

    @Mock
    private ISamObeAttainmentService obeAttainmentService;

    @InjectMocks
    private SamProgramQualityServiceImpl service;

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
    @DisplayName("majorId 为空：不可用且短路")
    void buildProfile_nullMajor()
    {
        Map<String, Object> r = service.buildProfile(null);
        assertFalse((Boolean) r.get("available"));
        verify(programQualityMapper, never()).selectMajorBase(anyLong());
    }

    @Test
    @DisplayName("专业不存在：available=false")
    void buildProfile_majorNotFound()
    {
        when(programQualityMapper.selectMajorBase(9L)).thenReturn(null);
        Map<String, Object> r = service.buildProfile(9L);
        assertFalse((Boolean) r.get("available"));
        assertNotNull(r.get("note"));
    }

    @Test
    @DisplayName("完整聚合：各板块齐备并计算完备度")
    void buildProfile_fullAggregation()
    {
        when(programQualityMapper.selectMajorBase(1L)).thenReturn(map("majorCode", "080601", "majorName", "计算机科学与技术", "deptId", 11L));
        when(programQualityMapper.selectStudentStats(1L)).thenReturn(map("totalCount", 100L, "enrolledCount", 80L));
        when(programQualityMapper.selectFacultySummary(11L)).thenReturn(map("teacherCount", 20L, "seniorCount", 8L));
        when(programQualityMapper.selectTitleDist(11L)).thenReturn(List.of(map("title", "教授", "cnt", 3L)));
        when(programQualityMapper.selectEducationDist(11L)).thenReturn(List.of(map("education", "博士", "cnt", 10L)));
        when(programQualityMapper.selectPublishedPlans(1L)).thenReturn(List.of(
                map("planId", 100L, "planName", "计算机2023版", "planYear", "2023"),
                map("planId", 90L, "planName", "计算机2020版", "planYear", "2020")));
        when(programQualityMapper.selectCreditSections(100L)).thenReturn(List.of(
                map("creditType", "REQUIRED", "creditTypeName", "专业核心", "requiredCredit", 60.0)));
        when(programQualityMapper.selectCourseStats(100L)).thenReturn(map("courseCount", 12L, "totalCredit", 150.0));
        when(programQualityMapper.selectOfferingStats(100L)).thenReturn(map("offeringCount", 30L));
        when(obeAttainmentService.dashboard(eq(100L), isNull())).thenReturn(map("totalRequirements", 5L, "achievedRequirements", 4L));
        when(programQualityMapper.selectEvalStats(11L)).thenReturn(map("evalCount", 50L, "avgScore", 88.5));
        when(programQualityMapper.selectSupervisionStats(11L)).thenReturn(map("recordCount", 12L, "avgScore", 90.0));
        when(programQualityMapper.selectSupervisionLevelDist(11L)).thenReturn(List.of(map("level", "0", "cnt", 6L)));

        Map<String, Object> r = service.buildProfile(1L);

        assertTrue((Boolean) r.get("available"));
        assertEquals("计算机科学与技术", ((Map<?, ?>) r.get("major")).get("majorName"));
        // 取最新方案（第一条）为口径
        assertEquals(100L, ((Map<?, ?>) r.get("plan")).get("planId"));
        assertEquals(1, ((List<?>) r.get("creditStructure")).size());
        assertEquals(4L, ((Map<?, ?>) r.get("obeAttainment")).get("achievedRequirements"));
        assertEquals(12L, ((Map<?, ?>) ((Map<?, ?>) r.get("supervision")).get("stats")).get("recordCount"));
        @SuppressWarnings("unchecked")
        Map<String, Object> c = (Map<String, Object>) r.get("completeness");
        assertEquals(7, c.get("totalSections"));
        assertEquals(7, c.get("coveredSections")); // 全部板块有数据
    }

    @Test
    @DisplayName("无已发布方案：OBE 短路且不查课程统计")
    void buildProfile_noPlan()
    {
        when(programQualityMapper.selectMajorBase(2L)).thenReturn(map("majorName", "数学", "deptId", 12L));
        when(programQualityMapper.selectStudentStats(2L)).thenReturn(map("totalCount", 0L));
        when(programQualityMapper.selectFacultySummary(12L)).thenReturn(map("teacherCount", 5L));
        when(programQualityMapper.selectTitleDist(12L)).thenReturn(new ArrayList<>());
        when(programQualityMapper.selectEducationDist(12L)).thenReturn(new ArrayList<>());
        when(programQualityMapper.selectPublishedPlans(2L)).thenReturn(new ArrayList<>());
        when(programQualityMapper.selectEvalStats(12L)).thenReturn(map("evalCount", 0L));
        when(programQualityMapper.selectSupervisionStats(12L)).thenReturn(map("recordCount", 0L));
        when(programQualityMapper.selectSupervisionLevelDist(12L)).thenReturn(new ArrayList<>());

        Map<String, Object> r = service.buildProfile(2L);

        assertTrue((Boolean) r.get("available"));
        assertNull(r.get("plan"));
        assertEquals(0, ((List<?>) r.get("creditStructure")).size());
        assertNull(r.get("obeAttainment"));
        verify(obeAttainmentService, never()).dashboard(anyLong(), isNull());
        verify(programQualityMapper, never()).selectCourseStats(anyLong());
    }
}
