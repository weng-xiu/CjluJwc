package com.yu.dis.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import com.yu.dis.quality.DisQualityService;
import com.yu.dis.quality.DisQualityVo;

/**
 * 数据对接质量看板接口层测试（Q1 覆盖率爬坡：MockMvc standalone，补齐 DisQualityController 缺口）。
 *
 * <p>N2 质量看板只读端点 GET /dis/quality/dashboard，校验路由绑定与 AjaxResult 包装契约
 * （成功码 200 + 看板 VO 落在 data）。聚合算法本身已由 {@code DisQualityServiceTest} 单测覆盖，
 * 本类仅锁定控制器层的出参结构，不重复断言计算逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisQualityControllerMockMvcTest {

    @Mock
    private DisQualityService disQualityService;

    @InjectMocks
    private DisQualityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("dashboard：返回成功码并把看板VO置于data")
    void dashboard_wrapsVoInData() throws Exception {
        DisQualityVo vo = new DisQualityVo();
        vo.setTaskCount(3);
        vo.setTotalExecute(200L);
        vo.setTotalFail(15L);
        vo.setSuccessRate(92.5D);
        vo.setWorstTaskName("成绩同步");
        when(disQualityService.dashboard()).thenReturn(vo);

        mockMvc.perform(get("/dis/quality/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskCount").value(3))
                .andExpect(jsonPath("$.data.successRate").value(92.5))
                .andExpect(jsonPath("$.data.worstTaskName").value("成绩同步"));

        verify(disQualityService).dashboard();
    }

    @Test
    @DisplayName("dashboard：无任务时看板VO安全返回（taskCount=0）")
    void dashboard_emptySafe() throws Exception {
        DisQualityVo vo = new DisQualityVo();
        when(disQualityService.dashboard()).thenReturn(vo);

        mockMvc.perform(get("/dis/quality/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskCount").value(0));
    }
}
