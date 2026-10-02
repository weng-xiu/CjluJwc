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

import com.yu.brm.domain.BrmTeacherQualification;
import com.yu.brm.service.IBrmTeacherQualificationService;

/**
 * 教师授课资格接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾教师子域控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/qualification、@Validated 必填校验
 * （teacherId @NotNull、courseCategory @NotBlank）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmTeacherQualificationControllerMockMvcTest {

    @Mock
    private IBrmTeacherQualificationService brmTeacherQualificationService;

    @InjectMocks
    private BrmTeacherQualificationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定qualId并返回资格记录")
    void getInfo_returnsQualification() throws Exception {
        BrmTeacherQualification qual = new BrmTeacherQualification();
        qual.setQualId(3L);
        qual.setCourseCategory("必修课");
        when(brmTeacherQualificationService.selectBrmTeacherQualificationByQualId(eq(3L))).thenReturn(qual);

        mockMvc.perform(get("/brm/qualification/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.courseCategory").value("必修课"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmTeacherQualificationService.insertBrmTeacherQualification(any(BrmTeacherQualification.class))).thenReturn(1);

        mockMvc.perform(post("/brm/qualification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":21,\"courseCategory\":\"必修课\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmTeacherQualification> captor = ArgumentCaptor.forClass(BrmTeacherQualification.class);
        verify(brmTeacherQualificationService).insertBrmTeacherQualification(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(21L), captor.getValue().getTeacherId());
        org.junit.jupiter.api.Assertions.assertEquals("必修课", captor.getValue().getCourseCategory());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/qualification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":21}"))
                .andExpect(status().isBadRequest());

        verify(brmTeacherQualificationService, never()).insertBrmTeacherQualification(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmTeacherQualificationService.updateBrmTeacherQualification(any(BrmTeacherQualification.class))).thenReturn(0);

        mockMvc.perform(put("/brm/qualification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qualId\":3,\"teacherId\":21,\"courseCategory\":\"选修课\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmTeacherQualificationService.deleteBrmTeacherQualificationByQualIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/qualification/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmTeacherQualificationService).deleteBrmTeacherQualificationByQualIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmTeacherQualificationService.selectBrmTeacherQualificationList(any(BrmTeacherQualification.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/qualification/list").param("teacherId", "21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmTeacherQualification> captor = ArgumentCaptor.forClass(BrmTeacherQualification.class);
        verify(brmTeacherQualificationService).selectBrmTeacherQualificationList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(21L), captor.getValue().getTeacherId());
    }
}
