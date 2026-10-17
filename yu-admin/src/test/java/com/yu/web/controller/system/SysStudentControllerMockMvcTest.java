package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.domain.SysStudent;
import com.yu.system.service.ISysStudentService;

/**
 * 学生账号管理控制器契约测试（G5）。
 * 校验 list/getInfo 绑定、add 中「学号唯一性」拦截优先于落库并回填 createBy、
 * changeStatus 将 studentId/studentStatus/remark 透传服务层、remove 逗号路径绑定 Long[]；
 * 视图层不感知账号与 BRM 学籍的关联与状态机细节。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysStudentControllerMockMvcTest
{
    @Mock
    private ISysStudentService studentService;

    @InjectMocks
    private SysStudentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    private SysStudent build(long id, String code)
    {
        SysStudent s = new SysStudent();
        s.setStudentId(id);
        s.setStudentCode(code);
        s.setNickName("张三");
        s.setStudentStatus("1");
        return s;
    }

    @Test
    @DisplayName("list：返回学生表格结构")
    void listReturnsTable() throws Exception
    {
        when(studentService.selectStudentList(any(SysStudent.class)))
                .thenReturn(List.of(build(1L, "2024001")));

        mockMvc.perform(get("/system/student/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].studentCode").value("2024001"));
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 studentId 返回详情")
    void getInfoBindsId() throws Exception
    {
        when(studentService.selectStudentById(9L)).thenReturn(build(9L, "2024009"));

        mockMvc.perform(get("/system/student/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentId").value(9))
                .andExpect(jsonPath("$.data.studentCode").value("2024009"));
    }

    @Test
    @DisplayName("add：学号已存在时拦截且不落库")
    void addDuplicateCodeRejected() throws Exception
    {
        when(studentService.checkStudentCodeUnique(any(SysStudent.class))).thenReturn(false);

        mockMvc.perform(post("/system/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"2024001\",\"nickName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增学生'2024001'失败，学号已存在"));

        verify(studentService, never()).insertStudent(any(SysStudent.class));
    }

    @Test
    @DisplayName("add：唯一性通过则回填 createBy 并落库")
    void addSuccessSetsCreateBy() throws Exception
    {
        when(studentService.checkStudentCodeUnique(any(SysStudent.class))).thenReturn(true);
        when(studentService.insertStudent(any(SysStudent.class))).thenReturn(1);

        mockMvc.perform(post("/system/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentCode\":\"2024002\",\"nickName\":\"李四\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysStudent> captor = ArgumentCaptor.forClass(SysStudent.class);
        verify(studentService).insertStudent(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("PUT /status：学籍状态三要素透传服务层")
    void changeStatusPassesThrough() throws Exception
    {
        when(studentService.changeStudentStatus(1L, "2", "休学")).thenReturn(1);

        mockMvc.perform(put("/system/student/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"studentStatus\":\"2\",\"remark\":\"休学\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(studentService).changeStudentStatus(1L, "2", "休学");
    }

    @Test
    @DisplayName("remove：逗号路径绑定 Long[] 并落删除")
    void removeBindsIds() throws Exception
    {
        when(studentService.deleteStudentByIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/system/student/3,4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(studentService).deleteStudentByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {3L, 4L}, captor.getValue());
    }
}
