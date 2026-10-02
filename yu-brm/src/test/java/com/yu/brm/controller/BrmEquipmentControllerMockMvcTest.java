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

import com.yu.brm.domain.BrmEquipment;
import com.yu.brm.service.IBrmEquipmentService;

/**
 * 多媒体设备接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾资产控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/equip、@Validated 必填校验
 * （equipName/equipType @NotBlank、classroomId @NotNull）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmEquipmentControllerMockMvcTest {

    @Mock
    private IBrmEquipmentService brmEquipmentService;

    @InjectMocks
    private BrmEquipmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定equipId并返回设备")
    void getInfo_returnsEquipment() throws Exception {
        BrmEquipment equip = new BrmEquipment();
        equip.setEquipId(9L);
        equip.setEquipName("投影仪");
        when(brmEquipmentService.selectBrmEquipmentByEquipId(eq(9L))).thenReturn(equip);

        mockMvc.perform(get("/brm/equip/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.equipName").value("投影仪"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmEquipmentService.insertBrmEquipment(any(BrmEquipment.class))).thenReturn(1);

        mockMvc.perform(post("/brm/equip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipName\":\"投影仪\",\"classroomId\":7,\"equipType\":\"投影\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmEquipment> captor = ArgumentCaptor.forClass(BrmEquipment.class);
        verify(brmEquipmentService).insertBrmEquipment(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getClassroomId());
        org.junit.jupiter.api.Assertions.assertEquals("投影", captor.getValue().getEquipType());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/equip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipName\":\"投影仪\"}"))
                .andExpect(status().isBadRequest());

        verify(brmEquipmentService, never()).insertBrmEquipment(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmEquipmentService.updateBrmEquipment(any(BrmEquipment.class))).thenReturn(0);

        mockMvc.perform(put("/brm/equip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipId\":9,\"equipName\":\"投影仪A\",\"classroomId\":7,\"equipType\":\"投影\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmEquipmentService.deleteBrmEquipmentByEquipIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/equip/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmEquipmentService).deleteBrmEquipmentByEquipIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmEquipmentService.selectBrmEquipmentList(any(BrmEquipment.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/equip/list").param("equipName", "投影"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmEquipment> captor = ArgumentCaptor.forClass(BrmEquipment.class);
        verify(brmEquipmentService).selectBrmEquipmentList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("投影", captor.getValue().getEquipName());
    }
}
