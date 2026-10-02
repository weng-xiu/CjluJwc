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

import com.yu.brm.domain.BrmSemester;
import com.yu.brm.service.IBrmSemesterService;

/**
 * 学期接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/semester、@Validated 必填校验
 * （semesterName @NotBlank、academicYearId @NotNull）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmSemesterControllerMockMvcTest {

    @Mock
    private IBrmSemesterService brmSemesterService;

    @InjectMocks
    private BrmSemesterController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定semesterId并返回学期")
    void getInfo_returnsSemester() throws Exception {
        BrmSemester semester = new BrmSemester();
        semester.setSemesterId(6L);
        semester.setSemesterName("2026秋");
        when(brmSemesterService.selectBrmSemesterBySemesterId(eq(6L))).thenReturn(semester);

        mockMvc.perform(get("/brm/semester/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.semesterName").value("2026秋"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmSemesterService.insertBrmSemester(any(BrmSemester.class))).thenReturn(1);

        mockMvc.perform(post("/brm/semester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterName\":\"2026秋\",\"academicYearId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmSemester> captor = ArgumentCaptor.forClass(BrmSemester.class);
        verify(brmSemesterService).insertBrmSemester(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026秋", captor.getValue().getSemesterName());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getAcademicYearId());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/semester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterName\":\"2026秋\"}"))
                .andExpect(status().isBadRequest());

        verify(brmSemesterService, never()).insertBrmSemester(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmSemesterService.updateBrmSemester(any(BrmSemester.class))).thenReturn(0);

        mockMvc.perform(put("/brm/semester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterId\":6,\"semesterName\":\"2026春\",\"academicYearId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmSemesterService.deleteBrmSemesterBySemesterIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/semester/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmSemesterService).deleteBrmSemesterBySemesterIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmSemesterService.selectBrmSemesterList(any(BrmSemester.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/semester/list").param("semesterName", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmSemester> captor = ArgumentCaptor.forClass(BrmSemester.class);
        verify(brmSemesterService).selectBrmSemesterList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("2026", captor.getValue().getSemesterName());
    }
}
