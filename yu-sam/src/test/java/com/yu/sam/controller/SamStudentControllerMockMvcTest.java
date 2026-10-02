package com.yu.sam.controller;

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

import com.yu.sam.domain.SamStudent;
import com.yu.sam.service.ISamStudentService;

/**
 * 学生学籍接口层测试（Q1 第十四批：MockMvc standalone，首次覆盖 yu-sam 业务域控制器）。
 * 校验路由 /sam/student、@Validated 必填校验（studentNo/studentName @NotBlank 与 majorId/deptId/classId @NotNull）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定；
 * P7 Excel 导入依赖真实 POI 解析与登录上下文，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentControllerMockMvcTest {

    @Mock
    private ISamStudentService samStudentService;

    @InjectMocks
    private SamStudentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定studentId并返回学籍数据体")
    void getInfo_returnsStudent() throws Exception {
        SamStudent student = new SamStudent();
        student.setStudentId(11L);
        student.setStudentNo("20210001");
        when(samStudentService.selectSamStudentByStudentId(eq(11L))).thenReturn(student);

        mockMvc.perform(get("/sam/student/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.studentId").value(11))
                .andExpect(jsonPath("$.data.studentNo").value("20210001"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(samStudentService.insertSamStudent(any(SamStudent.class))).thenReturn(1);

        String body = "{\"studentNo\":\"20210001\",\"studentName\":\"赵六\",\"majorId\":1,\"deptId\":2,\"classId\":3}";
        mockMvc.perform(post("/sam/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStudent> captor = ArgumentCaptor.forClass(SamStudent.class);
        verify(samStudentService).insertSamStudent(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("20210001", captor.getValue().getStudentNo());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getClassId());
    }

    @Test
    @DisplayName("add：缺失必填studentNo被@NotBlank拦截返回400，不落库")
    void add_missingStudentNoRejected() throws Exception {
        String body = "{\"studentName\":\"赵六\",\"majorId\":1,\"deptId\":2,\"classId\":3}";
        mockMvc.perform(post("/sam/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samStudentService, never()).insertSamStudent(any());
    }

    @Test
    @DisplayName("add：缺失必填classId被@NotNull拦截返回400，不落库")
    void add_missingClassIdRejected() throws Exception {
        String body = "{\"studentNo\":\"20210001\",\"studentName\":\"赵六\",\"majorId\":1,\"deptId\":2}";
        mockMvc.perform(post("/sam/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(samStudentService, never()).insertSamStudent(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(samStudentService.updateSamStudent(any(SamStudent.class))).thenReturn(0);

        String body = "{\"studentId\":11,\"studentNo\":\"20210001\",\"studentName\":\"赵六A\",\"majorId\":1,\"deptId\":2,\"classId\":3}";
        mockMvc.perform(put("/sam/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(samStudentService).updateSamStudent(any(SamStudent.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(samStudentService.deleteSamStudentByStudentIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/sam/student/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(samStudentService).deleteSamStudentByStudentIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(samStudentService.selectSamStudentList(any(SamStudent.class))).thenReturn(List.of());

        mockMvc.perform(get("/sam/student/list").param("studentName", "赵"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamStudent> captor = ArgumentCaptor.forClass(SamStudent.class);
        verify(samStudentService).selectSamStudentList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("赵", captor.getValue().getStudentName());
    }
}
