package com.yu.web.controller.system;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.web.service.ISubjectStatService;

/**
 * 主题分析控制器契约测试（G5，P3 只读跨模块聚合）。
 * 校验学生结构/成绩分析/师资分析三主题经统一 {code/data} 序列化，且可选学期参数原样透传服务层
 * （缺省即全量口径 null）；视图层不感知聚合内部来自哪些业务表与数据权限收敛。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysSubjectStatControllerMockMvcTest
{
    @Mock
    private ISubjectStatService subjectStatService;

    @InjectMocks
    private SysSubjectStatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /system/subjectStat/studentStructure：学生结构总览序列化")
    void studentStructure() throws Exception
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", 5000);
        r.put("male", 2600);
        when(subjectStatService.selectStudentStructure()).thenReturn(r);

        mockMvc.perform(get("/system/subjectStat/studentStructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(5000))
                .andExpect(jsonPath("$.data.male").value(2600));

        verify(subjectStatService).selectStudentStructure();
    }

    @Test
    @DisplayName("GET /gradeAnalysis：学期参数透传并回传统计")
    void gradeAnalysisWithSemester() throws Exception
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("avgScore", 75.5);
        when(subjectStatService.selectGradeAnalysis(3L)).thenReturn(r);

        mockMvc.perform(get("/system/subjectStat/gradeAnalysis").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avgScore").value(75.5));

        verify(subjectStatService).selectGradeAnalysis(3L);
    }

    @Test
    @DisplayName("GET /teacherStructure：缺省学期时以 null 全量口径透传")
    void teacherStructureNullSemester() throws Exception
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("teacherTotal", 320);
        when(subjectStatService.selectTeacherStructure(null)).thenReturn(r);

        mockMvc.perform(get("/system/subjectStat/teacherStructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherTotal").value(320));

        verify(subjectStatService).selectTeacherStructure(null);
    }
}
