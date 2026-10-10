package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.yu.common.exception.ServiceException;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.service.ISamStatusChangeService;
import com.yu.sam.service.ISamStudentService;

/**
 * 门户学籍异动接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验学生端防越权与异动申请闭环：list/info 先按 user_id 解析本人学籍、回退 student_id 口径、无学籍返回空；
 * changeList 强制绑定本人 studentId + createByExact；doApply 无学籍/异动类型非白名单(0休学1复学3退学)/原因为空
 * 逐条前置拒绝，合法则清空 changeId、回填本人 studentId 与原状态并提交多级审批流程；
 * trace/cancel 先做归属校验（admin 代管放行）。审批状态机/流程编排由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalStudentStatusControllerMockMvcTest {

    @Mock
    private ISamStudentService samStudentService;

    @Mock
    private ISamStatusChangeService samStatusChangeService;

    @InjectMocks
    private PortalStudentStatusController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        sysUser.setNickName("张同学");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SamStudent student(long studentId) {
        SamStudent s = new SamStudent();
        s.setStudentId(studentId);
        s.setStudentStatus("1");
        return s;
    }

    @Test
    @DisplayName("list：按 user_id 解析到本人学籍则返回单行")
    void list_resolvesByUserId() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(7L));

        mockMvc.perform(get("/portal/studentStatus/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].studentId").value(7));
    }

    @Test
    @DisplayName("list：user_id 未命中回退 student_id 口径")
    void list_fallbackByStudentId() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(null);
        when(samStudentService.selectSamStudentByStudentId(1L)).thenReturn(student(7L));

        mockMvc.perform(get("/portal/studentStatus/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].studentId").value(7));
    }

    @Test
    @DisplayName("list：无任何学籍档案返回空列表而非报错")
    void list_noStudentEmpty() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(null);
        when(samStudentService.selectSamStudentByStudentId(1L)).thenReturn(null);

        mockMvc.perform(get("/portal/studentStatus/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty());
    }

    @Test
    @DisplayName("changeList：强制绑定本人 studentId 与 createByExact")
    void changeList_forcesOwnScope() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(7L));
        when(samStatusChangeService.selectMyStatusChangeList(any(SamStatusChange.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/studentStatus/changeList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStatusChange> captor = ArgumentCaptor.forClass(SamStatusChange.class);
        verify(samStatusChangeService).selectMyStatusChangeList(captor.capture());
        Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getStudentId());
        Assertions.assertEquals("tester", captor.getValue().getCreateByExact());
    }

    @Test
    @DisplayName("change：合法休学申请清空 changeId、回填本人学籍并提交审批流程")
    void change_validSuspendAppliesAndSubmits() throws Exception {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(7L));

        mockMvc.perform(post("/portal/studentStatus/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeId\":999,\"changeType\":\"0\",\"reason\":\"身体原因\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamStatusChange> captor = ArgumentCaptor.forClass(SamStatusChange.class);
        verify(samStatusChangeService).insertSamStatusChange(captor.capture());
        SamStatusChange inserted = captor.getValue();
        Assertions.assertNull(inserted.getChangeId());
        Assertions.assertEquals(Long.valueOf(7L), inserted.getStudentId());
        Assertions.assertEquals("1", inserted.getOriginalStatus());
        Assertions.assertNull(inserted.getNewStatus());
        Assertions.assertEquals("tester", inserted.getCreateBy());
        verify(samStatusChangeService).submitForApproval(any());
    }

    @Test
    @DisplayName("change（直调）：异动类型非门户白名单被拒绝，不落库")
    void change_rejectsNonPortalType() {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(7L));
        SamStatusChange c = new SamStatusChange();
        c.setChangeType("2"); // 白名单外
        c.setReason("转专业");

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.change(c));
        Assertions.assertTrue(ex.getMessage().contains("门户端仅支持休学/复学/退学"));
        verify(samStatusChangeService, never()).insertSamStatusChange(any());
    }

    @Test
    @DisplayName("change（直调）：申请原因为空被拒绝")
    void change_rejectsBlankReason() {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(student(7L));
        SamStatusChange c = new SamStatusChange();
        c.setChangeType("0");
        c.setReason("   ");

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.change(c));
        Assertions.assertTrue(ex.getMessage().contains("申请原因不能为空"));
    }

    @Test
    @DisplayName("change（直调）：查无本人学籍被拒绝")
    void change_rejectsNoStudentProfile() {
        when(samStudentService.selectSamStudentByUserId(1L)).thenReturn(null);
        when(samStudentService.selectSamStudentByStudentId(1L)).thenReturn(null);
        SamStatusChange c = new SamStatusChange();
        c.setChangeType("0");
        c.setReason("身体原因");

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.change(c));
        Assertions.assertTrue(ex.getMessage().contains("未找到您的学籍信息"));
    }

    @Test
    @DisplayName("cancel：admin 代管放行归属校验后按本人撤销")
    void cancel_adminBypassesOwnership() throws Exception {
        SamStatusChange change = new SamStatusChange();
        change.setChangeId(5L);
        change.setStudentId(77L); // 非本人，但 admin 放行
        when(samStatusChangeService.selectSamStatusChangeByChangeId(5L)).thenReturn(change);
        when(samStatusChangeService.cancelByApplicant(eq(5L), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/portal/studentStatus/cancel/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(samStatusChangeService).cancelByApplicant(eq(5L), eq("tester"));
    }

    @Test
    @DisplayName("trace：归属校验通过后透传流程追溯结果")
    void trace_returnsFlowTrace() throws Exception {
        SamStatusChange change = new SamStatusChange();
        change.setChangeId(5L);
        when(samStatusChangeService.selectSamStatusChangeByChangeId(5L)).thenReturn(change);
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("nodes", 2);
        when(samStatusChangeService.traceChange(5L)).thenReturn(trace);

        mockMvc.perform(get("/portal/studentStatus/trace/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodes").value(2));
    }

    @Test
    @DisplayName("trace（直调）：申请不存在时归属校验拒绝")
    void trace_rejectsWhenAbsent() {
        when(samStatusChangeService.selectSamStatusChangeByChangeId(404L)).thenReturn(null);

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.trace(404L));
        Assertions.assertTrue(ex.getMessage().contains("异动申请不存在"));
        verify(samStatusChangeService, never()).traceChange(any());
    }
}
