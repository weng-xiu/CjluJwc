package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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

import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.mapper.SamStudentProfileMapper;
import com.yu.sam.service.ISamObeAttainmentService;

/**
 * N6 学生数字档案全息画像服务单测（只 mock 聚合 Mapper 与 OBE 服务）。
 *
 * @author N6
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentProfileServiceImplTest
{
    @Mock
    private SamStudentProfileMapper profileMapper;

    @Mock
    private ISamObeAttainmentService obeAttainmentService;

    @InjectMocks
    private SamStudentProfileServiceImpl service;

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
    @DisplayName("studentId 为空：不可用且短路")
    void buildProfile_nullStudent()
    {
        Map<String, Object> r = service.buildProfile(null);
        assertFalse((Boolean) r.get("available"));
        verify(profileMapper, never()).selectStudentBase(anyLong());
    }

    @Test
    @DisplayName("学生不存在：available=false")
    void buildProfile_studentNotFound()
    {
        when(profileMapper.selectStudentBase(9L)).thenReturn(null);
        Map<String, Object> r = service.buildProfile(9L);
        assertFalse((Boolean) r.get("available"));
        assertNotNull(r.get("note"));
    }

    @Test
    @DisplayName("完整聚合：各板块齐备并计算完备度")
    void buildProfile_fullAggregation()
    {
        when(profileMapper.selectStudentBase(1L)).thenReturn(map("studentNo", "2023001", "studentName", "张三"));
        when(profileMapper.selectAcademicSummary(1L)).thenReturn(map("courseCount", 8L, "gpa", 3.5, "earnedCredit", 20.0));
        when(profileMapper.selectPlanInfo(1L)).thenReturn(map("planId", 100L, "planName", "计算机2023"));
        when(profileMapper.selectCreditSections(100L)).thenReturn(List.of(
                map("creditType", "REQUIRED", "creditTypeName", "必修课", "requiredCredit", 6.0)));
        when(profileMapper.selectEarnedByCategory(1L)).thenReturn(List.of(
                map("creditType", "REQUIRED", "earnedCredit", 3.0)));
        when(obeAttainmentService.calculateByStudent(eq(100L), eq(1L))).thenReturn(List.of(new ObeAttainmentVo()));
        when(profileMapper.selectWarnings(1L)).thenReturn(List.of(map("warningLevel", "1")));
        when(profileMapper.selectReviewRecords(1L)).thenReturn(List.of(map("reviewType", "graduation")));
        when(profileMapper.selectThesis(1L)).thenReturn(map("topicName", "某论文"));
        when(profileMapper.selectRecentGrades(eq(1L), anyInt())).thenReturn(List.of(map("courseName", "数据结构")));

        Map<String, Object> r = service.buildProfile(1L);

        assertTrue((Boolean) r.get("available"));
        assertEquals("张三", ((Map<?, ?>) r.get("student")).get("studentName"));
        assertEquals(1, ((List<?>) r.get("creditStructure")).size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cs = (List<Map<String, Object>>) r.get("creditStructure");
        assertEquals(50.0, cs.get(0).get("rate")); // 3/6 = 50%
        assertEquals(1, ((List<?>) r.get("obeAttainment")).size());
        @SuppressWarnings("unchecked")
        Map<String, Object> c = (Map<String, Object>) r.get("completeness");
        assertEquals(9, c.get("totalSections"));
        assertEquals(9, c.get("coveredSections")); // 全部板块有数据
    }

    @Test
    @DisplayName("无培养方案：学分结构为空且不调用 OBE")
    void buildProfile_noPlan()
    {
        when(profileMapper.selectStudentBase(2L)).thenReturn(map("studentName", "李四"));
        when(profileMapper.selectAcademicSummary(2L)).thenReturn(map("courseCount", 0L));
        when(profileMapper.selectPlanInfo(2L)).thenReturn(null);
        when(profileMapper.selectWarnings(2L)).thenReturn(new ArrayList<>());
        when(profileMapper.selectReviewRecords(2L)).thenReturn(new ArrayList<>());
        when(profileMapper.selectThesis(2L)).thenReturn(null);
        when(profileMapper.selectRecentGrades(eq(2L), anyInt())).thenReturn(new ArrayList<>());

        Map<String, Object> r = service.buildProfile(2L);

        assertTrue((Boolean) r.get("available"));
        assertEquals(0, ((List<?>) r.get("creditStructure")).size());
        assertEquals(0, ((List<?>) r.get("obeAttainment")).size());
        verify(obeAttainmentService, never()).calculateByStudent(anyLong(), anyLong());
    }
}
