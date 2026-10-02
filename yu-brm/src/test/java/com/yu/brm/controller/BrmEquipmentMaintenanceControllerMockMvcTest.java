package com.yu.brm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.brm.domain.BrmEquipmentMaintenance;
import com.yu.brm.service.IBrmEquipmentMaintenanceService;

/**
 * 设备维护记录接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾资产控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/maintenance、@Validated 必填校验
 * （equipId @NotNull、faultDesc @NotBlank）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmEquipmentMaintenanceControllerMockMvcTest {

    @Mock
    private IBrmEquipmentMaintenanceService brmEquipmentMaintenanceService;

    @InjectMocks
    private BrmEquipmentMaintenanceController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定maintenanceId并返回维护记录")
    void getInfo_returnsMaintenance() throws Exception {
        BrmEquipmentMaintenance m = new BrmEquipmentMaintenance();
        m.setMaintenanceId(2L);
        m.setEquipId(9L);
        when(brmEquipmentMaintenanceService.selectBrmEquipmentMaintenanceByMaintenanceId(eq(2L))).thenReturn(m);

        mockMvc.perform(get("/brm/maintenance/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.equipId").value(9));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmEquipmentMaintenanceService.insertBrmEquipmentMaintenance(any(BrmEquipmentMaintenance.class))).thenReturn(1);

        mockMvc.perform(post("/brm/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipId\":9,\"faultDesc\":\"灯泡损坏\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmEquipmentMaintenance> captor = ArgumentCaptor.forClass(BrmEquipmentMaintenance.class);
        verify(brmEquipmentMaintenanceService).insertBrmEquipmentMaintenance(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getEquipId());
        org.junit.jupiter.api.Assertions.assertEquals("灯泡损坏", captor.getValue().getFaultDesc());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipId\":9}"))
                .andExpect(status().isBadRequest());

        verify(brmEquipmentMaintenanceService, never()).insertBrmEquipmentMaintenance(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmEquipmentMaintenanceService.updateBrmEquipmentMaintenance(any(BrmEquipmentMaintenance.class))).thenReturn(0);

        mockMvc.perform(put("/brm/maintenance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maintenanceId\":2,\"equipId\":9,\"faultDesc\":\"灯泡已换\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmEquipmentMaintenanceService.deleteBrmEquipmentMaintenanceByMaintenanceIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/maintenance/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmEquipmentMaintenanceService).deleteBrmEquipmentMaintenanceByMaintenanceIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmEquipmentMaintenanceService.selectBrmEquipmentMaintenanceList(any(BrmEquipmentMaintenance.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/maintenance/list").param("equipId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmEquipmentMaintenance> captor = ArgumentCaptor.forClass(BrmEquipmentMaintenance.class);
        verify(brmEquipmentMaintenanceService).selectBrmEquipmentMaintenanceList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(9L), captor.getValue().getEquipId());
    }
}
