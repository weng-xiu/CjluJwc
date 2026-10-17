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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.web.service.IDashboardStatService;

/**
 * 数据驾驶舱控制器契约测试（G5）：锁定管理端首页看板的总览聚合接口契约。
 * 校验当前学期、核心指标、预警分布等聚合数据经 {code/msg/data} 统一响应格式序列化下发，
 * 且视图层只读取聚合结果、不感知其内部计算与数据权限收敛逻辑。
 */
@ExtendWith(MockitoExtension.class)
class SysDashboardControllerMockMvcTest
{
    @Mock
    private IDashboardStatService dashboardStatService;

    @InjectMocks
    private SysDashboardController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /system/dashboard/overview：驾驶舱聚合数据经统一响应格式序列化")
    void overviewSerialisesAggregate() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("semesterName", "2025-2026学年第二学期");
        data.put("todayClass", 42);
        when(dashboardStatService.selectOverview()).thenReturn(data);

        mockMvc.perform(get("/system/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.semesterName").value("2025-2026学年第二学期"))
                .andExpect(jsonPath("$.data.todayClass").value(42));

        verify(dashboardStatService).selectOverview();
    }

    @Test
    @DisplayName("无数据时 overview 返回空聚合对象而非报错")
    void overviewEmptyReturnsMap() throws Exception
    {
        when(dashboardStatService.selectOverview()).thenReturn(new LinkedHashMap<>());

        mockMvc.perform(get("/system/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists());

        verify(dashboardStatService).selectOverview();
    }
}
