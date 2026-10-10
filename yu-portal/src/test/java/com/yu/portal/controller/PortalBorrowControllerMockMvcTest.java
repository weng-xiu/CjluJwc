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

import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.brm.service.IBrmClassroomService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.exception.ServiceException;
import com.yu.tpm.service.IBrmBorrowFlowService;

/**
 * 门户教室借用接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验防越权核心契约：myList 强制绑定登录用户 applicantUserId 并清空外部 applicant 参数；
 * apply 提交时登记人/状态/createBy 由登录态覆盖（忽略请求体伪造值）、必填校验前置拦截、
 * 提交后立即触达流程 submit；cancel/trace 先做归属校验（不存在即拒绝）；
 * classrooms 固定 status=0 只读下拉、occupancy/checkConflict 透传服务查询。
 * 两级审批流程与冲突判定业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalBorrowControllerMockMvcTest {

    @Mock
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @Mock
    private IBrmBorrowFlowService brmBorrowFlowService;

    @Mock
    private IBrmClassroomService brmClassroomService;

    @InjectMocks
    private PortalBorrowController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUserId() 依赖 SecurityContext，standalone 模式手动注入登录用户（userId=1）
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("classrooms：可选教室下拉固定 status=0 且返回分页行")
    void classrooms_forcesNormalStatus() throws Exception {
        BrmClassroom room = new BrmClassroom();
        room.setClassroomId(7L);
        when(brmClassroomService.selectBrmClassroomList(any(BrmClassroom.class)))
                .thenReturn(List.of(room));

        mockMvc.perform(get("/portal/borrow/classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows[0].classroomId").value(7));

        ArgumentCaptor<BrmClassroom> captor = ArgumentCaptor.forClass(BrmClassroom.class);
        verify(brmClassroomService).selectBrmClassroomList(captor.capture());
        Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("myList：applicantUserId 强制绑定登录用户、外部 applicant 参数被清空（防越权）")
    void myList_forcesOwnApplicantAndClearsName() throws Exception {
        when(brmClassroomBorrowService.selectBrmClassroomBorrowList(any(BrmClassroomBorrow.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/borrow/myList").param("applicant", "someoneElse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmClassroomBorrow> captor = ArgumentCaptor.forClass(BrmClassroomBorrow.class);
        verify(brmClassroomBorrowService).selectBrmClassroomBorrowList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getApplicantUserId());
        Assertions.assertNull(captor.getValue().getApplicant());
    }

    @Test
    @DisplayName("apply：登记人/状态/createBy 由登录态覆盖并立即提交流程")
    void apply_bindsLoginIdentityAndSubmitsFlow() throws Exception {
        BrmClassroomBorrow saved = new BrmClassroomBorrow();
        saved.setBorrowId(99L);
        when(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(any()))
                .thenReturn(saved);

        String body = "{\"classroomId\":7,\"borrowDate\":\"2026-10-01\",\"applicantUserId\":999,\"approveStatus\":\"2\"}";
        mockMvc.perform(post("/portal/borrow/apply")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmClassroomBorrow> captor = ArgumentCaptor.forClass(BrmClassroomBorrow.class);
        verify(brmClassroomBorrowService).insertBrmClassroomBorrow(captor.capture());
        BrmClassroomBorrow inserted = captor.getValue();
        // 请求体伪造的 applicantUserId/approveStatus 被登录态覆盖
        Assertions.assertEquals(Long.valueOf(1L), inserted.getApplicantUserId());
        Assertions.assertEquals("0", inserted.getApproveStatus());
        Assertions.assertEquals("tester", inserted.getCreateBy());
        Assertions.assertNull(inserted.getBorrowId());
        // 落库后立即启动两级审批流程
        verify(brmBorrowFlowService).submit(any());
    }

    @Test
    @DisplayName("apply（直调）：必填缺失抛出 ServiceException 且不落库、不启流程")
    void apply_directCallThrowsOnMissingRequired() {
        ServiceException ex = Assertions.assertThrows(ServiceException.class,
                () -> controller.apply(new BrmClassroomBorrow()));
        Assertions.assertTrue(ex.getMessage().contains("教室与借用日期不能为空"));
        verify(brmClassroomBorrowService, never()).insertBrmClassroomBorrow(any());
        verify(brmBorrowFlowService, never()).submit(any());
    }

    @Test
    @DisplayName("cancel：先做归属校验（申请存在）后走流程撤销")
    void cancel_checksOwnershipThenCancelsFlow() throws Exception {
        BrmClassroomBorrow owned = new BrmClassroomBorrow();
        owned.setBorrowId(5L);
        owned.setApplicantUserId(1L);
        when(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(5L)).thenReturn(owned);

        mockMvc.perform(put("/portal/borrow/cancel/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmBorrowFlowService).cancel(eq(5L));
    }

    @Test
    @DisplayName("trace：归属校验通过后透传全流程追溯结果")
    void trace_returnsFlowTrace() throws Exception {
        BrmClassroomBorrow owned = new BrmClassroomBorrow();
        owned.setBorrowId(5L);
        owned.setApplicantUserId(1L);
        when(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(5L)).thenReturn(owned);
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("nodeCount", 2);
        when(brmBorrowFlowService.trace(5L)).thenReturn(trace);

        mockMvc.perform(get("/portal/borrow/trace/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodeCount").value(2));
    }

    @Test
    @DisplayName("trace（直调）：申请不存在时归属校验拒绝，不触达流程")
    void trace_rejectsWhenBorrowAbsent() {
        when(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(404L)).thenReturn(null);

        ServiceException ex = Assertions.assertThrows(ServiceException.class,
                () -> controller.trace(404L));
        Assertions.assertTrue(ex.getMessage().contains("借用申请不存在"));
        verify(brmBorrowFlowService, never()).trace(any());
    }

    @Test
    @DisplayName("checkConflict：透传服务冲突预检（excludeBorrowId 恒 null）")
    void checkConflict_passesThroughService() throws Exception {
        when(brmClassroomBorrowService.checkConflict(eq(7L), any(), any(), any(), eq(null)))
                .thenReturn(List.of("10:00-12:00"));

        mockMvc.perform(get("/portal/borrow/checkConflict")
                        .param("classroomId", "7")
                        .param("borrowDate", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("10:00-12:00"));
    }
}
