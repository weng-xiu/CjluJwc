package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.aem.domain.AemGradeStatistics;
import com.yu.aem.service.IAemGradeStatisticsService;

/**
 * 成绩统计分析接口层测试（Q1 第十七批：MockMvc standalone）。
 * 只读聚合控制器，校验 /aem/gradeStatistics 各端点返回类型契约：
 * 域对象聚合结果落 $.data、纯文本回执（aggregateSemester）落 $.msg、分页统计落 $.rows/$.total、
 * 趋势全量数组落 $.data；getInfo 使用 statId:\\d+ 数字路径变量。聚合SQL算法由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeStatisticsControllerMockMvcTest {

    @Mock
    private IAemGradeStatisticsService aemGradeStatisticsService;

    @InjectMocks
    private AemGradeStatisticsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：数字路径变量绑定statId并返回快照数据体")
    void getInfo_returnsStat() throws Exception {
        AemGradeStatistics s = new AemGradeStatistics();
        s.setStatId(6L);
        s.setCourseId(3L);
        when(aemGradeStatisticsService.selectAemGradeStatisticsByStatId(eq(6L))).thenReturn(s);

        mockMvc.perform(get("/aem/gradeStatistics/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.statId").value(6))
                .andExpect(jsonPath("$.data.courseId").value(3));
    }

    @Test
    @DisplayName("aggregate：聚合快照为域对象，落data")
    void aggregate_returnsStat() throws Exception {
        AemGradeStatistics s = new AemGradeStatistics();
        s.setCourseId(2L);
        when(aemGradeStatisticsService.aggregateByCourse(eq(2L), eq(1L))).thenReturn(s);

        mockMvc.perform(post("/aem/gradeStatistics/aggregate").param("courseId", "2").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(2));

        verify(aemGradeStatisticsService).aggregateByCourse(eq(2L), eq(1L));
    }

    @Test
    @DisplayName("aggregateSemester：字符串回执落msg")
    void aggregateSemester_returnsMsg() throws Exception {
        when(aemGradeStatisticsService.aggregateBySemester(eq(1L))).thenReturn(7);

        mockMvc.perform(post("/aem/gradeStatistics/aggregateSemester").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("已聚合7门课程"));
    }

    @Test
    @DisplayName("distribution：Map结果落data")
    void distribution_returnsMap() throws Exception {
        when(aemGradeStatisticsService.scoreDistribution(eq(2L), eq(1L))).thenReturn(Map.of("90-100", 5));

        mockMvc.perform(get("/aem/gradeStatistics/distribution").param("courseId", "2").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[\"90-100\"]").value(5));
    }

    @Test
    @DisplayName("overview：学期总览Map落data")
    void overview_returnsMap() throws Exception {
        when(aemGradeStatisticsService.semesterOverview(eq(1L))).thenReturn(Map.of("avg", 82.0));

        mockMvc.perform(get("/aem/gradeStatistics/overview").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avg").value(82.0));
    }

    @Test
    @DisplayName("ranking：分页统计返回rows数组")
    void ranking_returnsRows() throws Exception {
        when(aemGradeStatisticsService.courseRanking(eq(2L), eq(1L)))
                .thenReturn(List.of(Map.of("studentId", 10)));

        mockMvc.perform(get("/aem/gradeStatistics/ranking").param("courseId", "2").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows[0].studentId").value(10));
    }

    @Test
    @DisplayName("byClass：courseId可选，缺省以null透传")
    void byClass_optionalCourseIdNull() throws Exception {
        when(aemGradeStatisticsService.statByClass(eq(1L), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/aem/gradeStatistics/byClass").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isArray());

        verify(aemGradeStatisticsService).statByClass(eq(1L), isNull());
    }

    @Test
    @DisplayName("trend：courseId可选，全量List落data数组")
    void trend_optionalCourseId() throws Exception {
        when(aemGradeStatisticsService.gradeTrend(isNull())).thenReturn(List.of(Map.of("semesterId", 1)));

        mockMvc.perform(get("/aem/gradeStatistics/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].semesterId").value(1));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemGradeStatisticsService.selectAemGradeStatisticsList(any(AemGradeStatistics.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/gradeStatistics/list").param("courseId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemGradeStatistics> captor = ArgumentCaptor.forClass(AemGradeStatistics.class);
        verify(aemGradeStatisticsService).selectAemGradeStatisticsList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getCourseId());
    }
}
