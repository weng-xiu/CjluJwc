package com.yu.tpm.controller;

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

import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.service.ITpmCourseLibraryService;

/**
 * 课程库接口层测试（Q1 第十三批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 必填校验（courseCode）、编辑 updateBy 语义、逗号数组批量删除、
 * list 条件绑定与表格结构；P7 批量导入的行级校验/唯一性/成败聚合逻辑由服务层覆盖
 * （导入依赖真实 POI 解析 Excel 输入流，不属纯接口层契约，此处不模拟）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmCourseLibraryControllerMockMvcTest {

    @Mock
    private ITpmCourseLibraryService tpmCourseLibraryService;

    @InjectMocks
    private TpmCourseLibraryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定courseId并返回课程数据体")
    void getInfo_returnsCourse() throws Exception {
        TpmCourseLibrary course = new TpmCourseLibrary();
        course.setCourseId(11L);
        course.setCourseCode("MATH101");
        course.setCourseName("高等数学");
        when(tpmCourseLibraryService.selectTpmCourseLibraryByCourseId(eq(11L))).thenReturn(course);

        mockMvc.perform(get("/tpm/courseLib/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.courseId").value(11))
                .andExpect(jsonPath("$.data.courseCode").value("MATH101"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmCourseLibraryService.insertTpmCourseLibrary(any(TpmCourseLibrary.class))).thenReturn(1);

        String body = "{\"courseCode\":\"MATH101\",\"courseName\":\"高等数学\",\"credit\":4}";
        mockMvc.perform(post("/tpm/courseLib")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCourseLibrary> captor = ArgumentCaptor.forClass(TpmCourseLibrary.class);
        verify(tpmCourseLibraryService).insertTpmCourseLibrary(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("MATH101", captor.getValue().getCourseCode());
    }

    @Test
    @DisplayName("add：缺失必填courseCode被@NotBlank拦截返回400，不落库")
    void add_missingCourseCodeRejected() throws Exception {
        String body = "{\"courseName\":\"高等数学\",\"credit\":4}";
        mockMvc.perform(post("/tpm/courseLib")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCourseLibraryService, never()).insertTpmCourseLibrary(any());
    }

    @Test
    @DisplayName("edit：合法请求体走更新服务并回填")
    void edit_validBodyUpdates() throws Exception {
        when(tpmCourseLibraryService.updateTpmCourseLibrary(any(TpmCourseLibrary.class))).thenReturn(1);

        String body = "{\"courseId\":11,\"courseCode\":\"MATH101\",\"courseName\":\"线性代数\"}";
        mockMvc.perform(put("/tpm/courseLib")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmCourseLibrary> captor = ArgumentCaptor.forClass(TpmCourseLibrary.class);
        verify(tpmCourseLibraryService).updateTpmCourseLibrary(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("线性代数", captor.getValue().getCourseName());
    }

    @Test
    @DisplayName("edit：courseCode为空被@NotBlank拦截返回400，不更新")
    void edit_blankCourseCodeRejected() throws Exception {
        String body = "{\"courseId\":11,\"courseName\":\"线性代数\"}";
        mockMvc.perform(put("/tpm/courseLib")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmCourseLibraryService, never()).updateTpmCourseLibrary(any());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(tpmCourseLibraryService.deleteTpmCourseLibraryByCourseIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/tpm/courseLib/5,6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(tpmCourseLibraryService).deleteTpmCourseLibraryByCourseIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("remove：影响行数0时toAjax降级为error(500)")
    void remove_zeroRowsDegrades() throws Exception {
        when(tpmCourseLibraryService.deleteTpmCourseLibraryByCourseIds(any(Long[].class))).thenReturn(0);

        mockMvc.perform(delete("/tpm/courseLib/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(tpmCourseLibraryService.selectTpmCourseLibraryList(any(TpmCourseLibrary.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/tpm/courseLib/list").param("courseName", "数学"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmCourseLibrary> captor = ArgumentCaptor.forClass(TpmCourseLibrary.class);
        verify(tpmCourseLibraryService).selectTpmCourseLibraryList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("数学", captor.getValue().getCourseName());
    }
}
