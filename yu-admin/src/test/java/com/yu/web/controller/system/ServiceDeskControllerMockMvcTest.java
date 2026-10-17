package com.yu.web.controller.system;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.servicedesk.IServiceDeskService;
import com.yu.system.servicedesk.ServiceDashboardVo;

/**
 * 一站式服务大厅控制器契约测试（G5）：锁定事项目录、时效看板、超时催办三条管理端接口契约。
 * 校验目录/看板经统一响应格式序列化，催办复用 AjaxResult.success(msg, data) 回传被催办数量，
 * 视图层不感知服务层事项注册与事件通道下发细节。
 */
@ExtendWith(MockitoExtension.class)
class ServiceDeskControllerMockMvcTest
{
    @Mock
    private IServiceDeskService serviceDeskService;

    @InjectMocks
    private ServiceDeskController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /system/serviceDesk/catalog：事项目录经统一响应格式序列化")
    void catalogSerialisesData() throws Exception
    {
        when(serviceDeskService.catalog()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/serviceDesk/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());

        verify(serviceDeskService).catalog();
    }

    @Test
    @DisplayName("GET /system/serviceDesk/dashboard：时效看板档位统计原样序列化")
    void dashboardSerialisesVo() throws Exception
    {
        ServiceDashboardVo vo = new ServiceDashboardVo();
        vo.setTotalPending(10);
        vo.setNormalCount(6);
        vo.setNearDueCount(2);
        vo.setOverdueCount(2);
        when(serviceDeskService.dashboard()).thenReturn(vo);

        mockMvc.perform(get("/system/serviceDesk/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalPending").value(10))
                .andExpect(jsonPath("$.data.normalCount").value(6))
                .andExpect(jsonPath("$.data.nearDueCount").value(2))
                .andExpect(jsonPath("$.data.overdueCount").value(2));

        verify(serviceDeskService).dashboard();
    }

    @Test
    @DisplayName("POST /system/serviceDesk/urge：催办回传统一话术与被催办数量")
    void urgeReturnsCount() throws Exception
    {
        when(serviceDeskService.urgeOverdue()).thenReturn(3);

        mockMvc.perform(post("/system/serviceDesk/urge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("催办完成"))
                .andExpect(jsonPath("$.data").value(3));

        verify(serviceDeskService).urgeOverdue();
    }
}
