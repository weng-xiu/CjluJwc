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

import com.yu.brm.domain.BrmBuilding;
import com.yu.brm.service.IBrmBuildingService;

/**
 * 教学楼接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/building、@Validated 必填校验
 * （buildingName/buildingCode @NotBlank、campusId @NotNull）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmBuildingControllerMockMvcTest {

    @Mock
    private IBrmBuildingService brmBuildingService;

    @InjectMocks
    private BrmBuildingController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定buildingId并返回教学楼")
    void getInfo_returnsBuilding() throws Exception {
        BrmBuilding building = new BrmBuilding();
        building.setBuildingId(6L);
        building.setBuildingName("一教");
        when(brmBuildingService.selectBrmBuildingByBuildingId(eq(6L))).thenReturn(building);

        mockMvc.perform(get("/brm/building/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.buildingName").value("一教"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmBuildingService.insertBrmBuilding(any(BrmBuilding.class))).thenReturn(1);

        mockMvc.perform(post("/brm/building")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingName\":\"一教\",\"buildingCode\":\"B01\",\"campusId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmBuilding> captor = ArgumentCaptor.forClass(BrmBuilding.class);
        verify(brmBuildingService).insertBrmBuilding(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("B01", captor.getValue().getBuildingCode());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getCampusId());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/building")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingName\":\"一教\"}"))
                .andExpect(status().isBadRequest());

        verify(brmBuildingService, never()).insertBrmBuilding(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmBuildingService.updateBrmBuilding(any(BrmBuilding.class))).thenReturn(0);

        mockMvc.perform(put("/brm/building")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"buildingId\":6,\"buildingName\":\"一教A\",\"buildingCode\":\"B01\",\"campusId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmBuildingService.deleteBrmBuildingByBuildingIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/building/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmBuildingService).deleteBrmBuildingByBuildingIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmBuildingService.selectBrmBuildingList(any(BrmBuilding.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/building/list").param("buildingName", "一教"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmBuilding> captor = ArgumentCaptor.forClass(BrmBuilding.class);
        verify(brmBuildingService).selectBrmBuildingList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("一教", captor.getValue().getBuildingName());
    }
}
