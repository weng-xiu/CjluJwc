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

import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.service.IBrmTeacherService;

/**
 * 教师接口层测试（Q1 第十四批：MockMvc standalone，首次覆盖 yu-brm 业务域控制器）。
 * 校验 HTTP 路由、@Validated 必填校验（teacherCode/teacherName @NotBlank、deptId @NotNull）、
 * add/edit 域对象透传、逗号数组批量删除契约、list 查询条件绑定；
 * P7 Excel 导入依赖真实 POI 解析输入流与 SecurityUtils 登录上下文，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmTeacherControllerMockMvcTest {

    @Mock
    private IBrmTeacherService brmTeacherService;

    @InjectMocks
    private BrmTeacherController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定teacherId并返回教师数据体")
    void getInfo_returnsTeacher() throws Exception {
        BrmTeacher teacher = new BrmTeacher();
        teacher.setTeacherId(7L);
        teacher.setTeacherName("张三");
        when(brmTeacherService.selectBrmTeacherByTeacherId(eq(7L))).thenReturn(teacher);

        mockMvc.perform(get("/brm/teacher/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.teacherId").value(7))
                .andExpect(jsonPath("$.data.teacherName").value("张三"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmTeacherService.insertBrmTeacher(any(BrmTeacher.class))).thenReturn(1);

        String body = "{\"teacherCode\":\"T001\",\"teacherName\":\"李四\",\"deptId\":3}";
        mockMvc.perform(post("/brm/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmTeacher> captor = ArgumentCaptor.forClass(BrmTeacher.class);
        verify(brmTeacherService).insertBrmTeacher(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("T001", captor.getValue().getTeacherCode());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getDeptId());
    }

    @Test
    @DisplayName("add：缺失必填teacherCode被@NotBlank拦截返回400，不落库")
    void add_missingTeacherCodeRejected() throws Exception {
        String body = "{\"teacherName\":\"李四\",\"deptId\":3}";
        mockMvc.perform(post("/brm/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmTeacherService, never()).insertBrmTeacher(any());
    }

    @Test
    @DisplayName("add：缺失必填deptId被@NotNull拦截返回400，不落库")
    void add_missingDeptIdRejected() throws Exception {
        String body = "{\"teacherCode\":\"T001\",\"teacherName\":\"李四\"}";
        mockMvc.perform(post("/brm/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmTeacherService, never()).insertBrmTeacher(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmTeacherService.updateBrmTeacher(any(BrmTeacher.class))).thenReturn(0);

        String body = "{\"teacherId\":7,\"teacherCode\":\"T001\",\"teacherName\":\"王五\",\"deptId\":3}";
        mockMvc.perform(put("/brm/teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(brmTeacherService).updateBrmTeacher(any(BrmTeacher.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmTeacherService.deleteBrmTeacherByTeacherIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/teacher/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmTeacherService).deleteBrmTeacherByTeacherIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue()[0]);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmTeacherService.selectBrmTeacherList(any(BrmTeacher.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/teacher/list").param("teacherName", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmTeacher> captor = ArgumentCaptor.forClass(BrmTeacher.class);
        verify(brmTeacherService).selectBrmTeacherList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("张", captor.getValue().getTeacherName());
    }
}
