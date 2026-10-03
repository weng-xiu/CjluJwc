package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import com.yu.aem.service.IAemEvaluationStatService;

/**
 * 评教统计接口层测试（Q1 第十七批：MockMvc standalone）。
 * 只读聚合控制器，校验 /aem/evaluationStat 六端点的可选 @RequestParam 透传契约：
 * 缺省参数以 null 传入服务层（全量口径），Map 结果落 $.data、List 结果落 $.data 数组；
 * 聚合算法本身由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemEvaluationStatControllerMockMvcTest {

    @Mock
    private IAemEvaluationStatService aemEvaluationStatService;

    @InjectMocks
    private AemEvaluationStatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("overview：命中两可选参数并透传，Map落data")
    void overview_passthroughParams() throws Exception {
        when(aemEvaluationStatService.overview(eq(1L), eq(2L))).thenReturn(Map.of("avgScore", 88.5));

        mockMvc.perform(get("/aem/evaluationStat/overview").param("questionnaireId", "1").param("semesterId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.avgScore").value(88.5));

        verify(aemEvaluationStatService).overview(eq(1L), eq(2L));
    }

    @Test
    @DisplayName("overview：不传参数时以null透传（全量口径）")
    void overview_nullParams() throws Exception {
        when(aemEvaluationStatService.overview(isNull(), isNull())).thenReturn(Map.of("total", 0));

        mockMvc.perform(get("/aem/evaluationStat/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(aemEvaluationStatService).overview(isNull(), isNull());
    }

    @Test
    @DisplayName("byCourse：List<Map>聚合结果落data数组")
    void byCourse_returnsList() throws Exception {
        when(aemEvaluationStatService.byCourse(eq(1L), isNull())).thenReturn(List.of(Map.of("courseId", 7)));

        mockMvc.perform(get("/aem/evaluationStat/byCourse").param("questionnaireId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].courseId").value(7));
    }

    @Test
    @DisplayName("byTeacher：教师聚合透传两可选参数")
    void byTeacher_passthrough() throws Exception {
        when(aemEvaluationStatService.byTeacher(eq(3L), eq(4L))).thenReturn(List.of());

        mockMvc.perform(get("/aem/evaluationStat/byTeacher").param("questionnaireId", "3").param("semesterId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(aemEvaluationStatService).byTeacher(eq(3L), eq(4L));
    }

    @Test
    @DisplayName("byClass：三可选参数（含courseId）全null透传")
    void byClass_allNull() throws Exception {
        when(aemEvaluationStatService.byClass(isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/aem/evaluationStat/byClass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(aemEvaluationStatService).byClass(isNull(), isNull(), isNull());
    }

    @Test
    @DisplayName("commentAnalysis：三参数透传，Map落data")
    void commentAnalysis_passthrough() throws Exception {
        when(aemEvaluationStatService.commentAnalysis(eq(5L), eq(6L), eq(7L))).thenReturn(Map.of("topWord", "认真"));

        mockMvc.perform(get("/aem/evaluationStat/commentAnalysis")
                        .param("teacherId", "5").param("courseId", "6").param("questionnaireId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topWord").value("认真"));
    }
}
