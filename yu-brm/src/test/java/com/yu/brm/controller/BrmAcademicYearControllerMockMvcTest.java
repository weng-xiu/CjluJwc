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

import com.yu.brm.domain.BrmAcademicYear;
import com.yu.brm.service.IBrmAcademicYearService;

/**
 * 学年接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/year、@Validated 必填校验（yearName @NotBlank）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmAcademicYearControllerMockMvcTest {

    @Mock
    private IBrmAcademicYearService brmAcademicYearService;

    @InjectMocks
    private BrmAcademicYearController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定yearId并返回学年")
    void getInfo_returnsYear() throws Exception {
        BrmAcademicYear year = new BrmAcademicYear();
        year.setYearId(3L);
        year.setYearName("2026-2027");
        when(brmAcademicYearService.selectBrmAcademicYearByYearId(eq(3L))).thenReturn(year);

        mockMvc.perform(get("/brm/year/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.yearName").value("2026-2027"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmAcademicYearService.insertBrmAcademicYear(any(BrmAcademicYear.class))).thenReturn(1);

        mockMvc.perform(post("/brm/year")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"yearName\":\"2026-2027\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmAcademicYear> captor = ArgumentCaptor.forClass(BrmAcademicYear.class);
        verify(brmAcademicYearService).insertBrmAcademicYear(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026-2027", captor.getValue().getYearName());
    }

    @Test
    @DisplayName("add：缺失必填yearName被@NotBlank拦截返回400，不落库")
    void add_missingYearNameRejected() throws Exception {
        mockMvc.perform(post("/brm/year")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(brmAcademicYearService, never()).insertBrmAcademicYear(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmAcademicYearService.updateBrmAcademicYear(any(BrmAcademicYear.class))).thenReturn(0);

        mockMvc.perform(put("/brm/year")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"yearId\":3,\"yearName\":\"2026-2027A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmAcademicYearService.deleteBrmAcademicYearByYearIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/year/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmAcademicYearService).deleteBrmAcademicYearByYearIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmAcademicYearService.selectBrmAcademicYearList(any(BrmAcademicYear.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/year/list").param("yearName", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmAcademicYear> captor = ArgumentCaptor.forClass(BrmAcademicYear.class);
        verify(brmAcademicYearService).selectBrmAcademicYearList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026", captor.getValue().getYearName());
    }
}
