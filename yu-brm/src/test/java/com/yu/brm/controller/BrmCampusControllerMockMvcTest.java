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

import com.yu.brm.domain.BrmCampus;
import com.yu.brm.service.IBrmCampusService;

/**
 * 校区接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/campus、@Validated 必填校验（campusName @NotBlank）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmCampusControllerMockMvcTest {

    @Mock
    private IBrmCampusService brmCampusService;

    @InjectMocks
    private BrmCampusController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定campusId并返回校区")
    void getInfo_returnsCampus() throws Exception {
        BrmCampus campus = new BrmCampus();
        campus.setCampusId(2L);
        campus.setCampusName("西校区");
        when(brmCampusService.selectBrmCampusByCampusId(eq(2L))).thenReturn(campus);

        mockMvc.perform(get("/brm/campus/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.campusName").value("西校区"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmCampusService.insertBrmCampus(any(BrmCampus.class))).thenReturn(1);

        mockMvc.perform(post("/brm/campus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campusName\":\"西校区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmCampus> captor = ArgumentCaptor.forClass(BrmCampus.class);
        verify(brmCampusService).insertBrmCampus(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("西校区", captor.getValue().getCampusName());
    }

    @Test
    @DisplayName("add：缺失必填campusName被@NotBlank拦截返回400，不落库")
    void add_missingCampusNameRejected() throws Exception {
        mockMvc.perform(post("/brm/campus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(brmCampusService, never()).insertBrmCampus(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmCampusService.updateBrmCampus(any(BrmCampus.class))).thenReturn(0);

        mockMvc.perform(put("/brm/campus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campusId\":2,\"campusName\":\"西校区A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmCampusService.deleteBrmCampusByCampusIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/campus/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmCampusService).deleteBrmCampusByCampusIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmCampusService.selectBrmCampusList(any(BrmCampus.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/campus/list").param("campusName", "校区"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmCampus> captor = ArgumentCaptor.forClass(BrmCampus.class);
        verify(brmCampusService).selectBrmCampusList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("校区", captor.getValue().getCampusName());
    }
}
