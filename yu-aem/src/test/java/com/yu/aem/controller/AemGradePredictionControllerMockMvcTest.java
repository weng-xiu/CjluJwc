package com.yu.aem.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
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

import com.yu.aem.service.IAemGradePredictionService;

/**
 * 成绩与学业预测接口层测试（Q1 第十三批：MockMvc standalone，覆盖 F2-3 新增只读分析控制器）。
 * 校验三个只读端点的路由与 @RequestParam 绑定契约：studentRisk 必填 studentId（缺失→400）、
 * courseDifficulty 的 semesterId 可选（缺省以 null 透传=全量口径）、
 * riskBoard 必填 semesterId + 可选 classId；预测算法本身由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradePredictionControllerMockMvcTest {

    @Mock
    private IAemGradePredictionService gradePredictionService;

    @InjectMocks
    private AemGradePredictionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("studentRisk：必填studentId透传，预测结果Map落data")
    void studentRisk_passthroughAndData() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("riskLevel", "HIGH");
        result.put("predictedNextGpa", 1.8);
        when(gradePredictionService.predictStudentRisk(eq(10L))).thenReturn(result);

        mockMvc.perform(get("/aem/gradePrediction/studentRisk").param("studentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.riskLevel").value("HIGH"));

        verify(gradePredictionService).predictStudentRisk(eq(10L));
    }

    @Test
    @DisplayName("studentRisk：缺失必填studentId返回400，不调用服务")
    void studentRisk_missingParamRejected() throws Exception {
        mockMvc.perform(get("/aem/gradePrediction/studentRisk"))
                .andExpect(status().isBadRequest());

        verify(gradePredictionService, never()).predictStudentRisk(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("courseDifficulty：不传semesterId以null透传（全量口径）")
    void courseDifficulty_optionalSemesterNullPassthrough() throws Exception {
        when(gradePredictionService.courseDifficultyProfile(eq(null)))
                .thenReturn(List.of(Map.of("courseName", "高等数学", "difficultyIndex", 72)));

        mockMvc.perform(get("/aem/gradePrediction/courseDifficulty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].difficultyIndex").value(72));

        verify(gradePredictionService).courseDifficultyProfile(eq(null));
    }

    @Test
    @DisplayName("courseDifficulty：传semesterId时按学期口径透传")
    void courseDifficulty_withSemesterBound() throws Exception {
        when(gradePredictionService.courseDifficultyProfile(eq(2L))).thenReturn(List.of());

        mockMvc.perform(get("/aem/gradePrediction/courseDifficulty").param("semesterId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(gradePredictionService).courseDifficultyProfile(eq(2L));
    }

    @Test
    @DisplayName("riskBoard：必填semesterId + 可选classId缺省null透传")
    void riskBoard_requiredSemesterOptionalClass() throws Exception {
        Map<String, Object> board = new LinkedHashMap<>();
        board.put("total", 128);
        board.put("distribution", Map.of("HIGH", 5, "LOW", 90));
        when(gradePredictionService.riskBoard(eq(3L), eq(null))).thenReturn(board);

        mockMvc.perform(get("/aem/gradePrediction/riskBoard").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(128));

        verify(gradePredictionService).riskBoard(eq(3L), eq(null));
    }

    @Test
    @DisplayName("riskBoard：缺失必填semesterId返回400，不调用服务")
    void riskBoard_missingSemesterRejected() throws Exception {
        mockMvc.perform(get("/aem/gradePrediction/riskBoard"))
                .andExpect(status().isBadRequest());

        verify(gradePredictionService, never()).riskBoard(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
