package com.yu.brm.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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

import com.yu.brm.service.IBrmResourceStatService;

/**
 * 资源利用分析接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm B2 资源统计控制器）。
 * 只读分析型控制器，无 CRUD。校验路由 /brm/resourceStat 下 overview/classroomUtilization/
 * teacherWorkload/maintenanceDue 四个 GET 端点的参数绑定与服务透传；
 * semesterId/buildingId 均为可选 @RequestParam，缺省不报错；
 * 服务返回聚合对象未打桩即为 null，success(null) 仍返回 200，故仅断言状态码与服务被调用。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmResourceStatControllerMockMvcTest {

    @Mock
    private IBrmResourceStatService brmResourceStatService;

    @InjectMocks
    private BrmResourceStatController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("overview：可选semesterId透传并返回资源总览")
    void overview_passesSemesterId() throws Exception {
        mockMvc.perform(get("/brm/resourceStat/overview").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmResourceStatService).overview(eq(3L));
    }

    @Test
    @DisplayName("overview：缺省semesterId不报错")
    void overview_withoutParam() throws Exception {
        mockMvc.perform(get("/brm/resourceStat/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmResourceStatService).overview(isNull());
    }

    @Test
    @DisplayName("classroomUtilization：按semesterId与buildingId双参透传")
    void classroomUtilization_passesBothParams() throws Exception {
        mockMvc.perform(get("/brm/resourceStat/classroomUtilization")
                        .param("semesterId", "3").param("buildingId", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmResourceStatService).classroomUtilization(eq(3L), eq(6L));
    }

    @Test
    @DisplayName("teacherWorkload：按semesterId透传")
    void teacherWorkload_passesSemesterId() throws Exception {
        mockMvc.perform(get("/brm/resourceStat/teacherWorkload").param("semesterId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmResourceStatService).teacherWorkload(eq(3L));
    }

    @Test
    @DisplayName("maintenanceDue：无参触发维保到期提醒")
    void maintenanceDue_noArg() throws Exception {
        mockMvc.perform(get("/brm/resourceStat/maintenanceDue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmResourceStatService).maintenanceDue();
    }
}
