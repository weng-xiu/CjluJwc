package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.service.ISamObeAttainmentService;

/**
 * OBE 达成度接口层测试（N1，MockMvc standalone）。
 * 校验 HTTP 路由、参数绑定与 JSON 响应契约，业务与算法由服务/计算器单测覆盖。
 *
 * @author N1
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamObeControllerMockMvcTest
{
    @Mock
    private ISamObeAttainmentService obeService;

    @InjectMocks
    private SamObeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("dashboard：planId/semesterId 绑定并返回汇总")
    void dashboard_bindsParams() throws Exception
    {
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("planId", 1L);
        dash.put("totalRequirements", 3);
        dash.put("achievedRequirements", 2);
        when(obeService.dashboard(eq(1L), eq(7L))).thenReturn(dash);

        mockMvc.perform(get("/sam/obe/dashboard").param("planId", "1").param("semesterId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRequirements").value(3))
                .andExpect(jsonPath("$.data.achievedRequirements").value(2));

        verify(obeService).dashboard(eq(1L), eq(7L));
    }

    @Test
    @DisplayName("calculate/student：透传 planId 与 studentId")
    void calculateStudent_delegates() throws Exception
    {
        List<ObeAttainmentVo> res = new ArrayList<>();
        ObeAttainmentVo vo = new ObeAttainmentVo();
        vo.setGrId(1L);
        vo.setAttainment(0.82);
        vo.setAchieved("1");
        res.add(vo);
        when(obeService.calculateByStudent(eq(1L), eq(9L))).thenReturn(res);

        mockMvc.perform(get("/sam/obe/calculate/student").param("planId", "1").param("studentId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].attainment").value(0.82))
                .andExpect(jsonPath("$.data[0].achieved").value("1"));

        verify(obeService).calculateByStudent(eq(1L), eq(9L));
    }

    @Test
    @DisplayName("requirement 新增：请求体绑定并返回成功码")
    void requirementAdd_returnsOk() throws Exception
    {
        when(obeService.insertRequirement(any(SamObeGraduationRequirement.class))).thenReturn(1);

        mockMvc.perform(post("/sam/obe/requirement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":1,\"grCode\":\"GR-01\",\"grName\":\"工程知识\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(obeService).insertRequirement(any(SamObeGraduationRequirement.class));
    }

    @Test
    @DisplayName("support 删除：路径变量绑定为数组透传")
    void supportRemove_bindsArray() throws Exception
    {
        when(obeService.deleteSupportBySupportIds(any())).thenReturn(2);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/sam/obe/support/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        org.mockito.ArgumentCaptor<Long[]> cap = org.mockito.ArgumentCaptor.forClass(Long[].class);
        verify(obeService).deleteSupportBySupportIds(cap.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Long[] {1L, 2L}, cap.getValue());
        verify(obeService, never()).insertSupport(any());
    }
}
