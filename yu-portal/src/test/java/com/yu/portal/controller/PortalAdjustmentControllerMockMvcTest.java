package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import com.yu.common.exception.ServiceException;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 门户调停课申请接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验教师端防越权与审批闭环契约：list 非管理员强制绑定 createByExact=登录名、管理员放行；
 * mySchedules 按登录教师 teacherId+status=0 过滤；add 管理员豁免排课归属、
 * 登记人/createBy/approveStatus 由登录态覆盖、非法调课类型与空原因前置拒绝；
 * cancel 归属校验后走本人撤销；pendingList/approve/reject 仅审批人(admin 放行)可用，
 * 待审列表固定 approveStatus=0。审批回写/待办通知等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalAdjustmentControllerMockMvcTest {

    @Mock
    private ITpmScheduleAdjustmentService tpmScheduleAdjustmentService;

    @Mock
    private ITpmScheduleService tpmScheduleService;

    @Mock
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @InjectMocks
    private PortalAdjustmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        loginAs(1L); // 默认 admin（userId=1）
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** standalone 下手动注入登录态；userId=1 为 admin，其余为普通教师 */
    private void loginAs(Long userId) {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(userId, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @Test
    @DisplayName("list：普通教师被强制绑定 createByExact=登录名（防越权）")
    void list_nonAdminForcesOwnCreator() throws Exception {
        loginAs(100L);
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(any(TpmScheduleAdjustment.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/adjustment/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).selectTpmScheduleAdjustmentList(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateByExact());
    }

    @Test
    @DisplayName("list：管理员不绑定归属，可查看全量")
    void list_adminSeesAll() throws Exception {
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(any(TpmScheduleAdjustment.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/adjustment/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).selectTpmScheduleAdjustmentList(captor.capture());
        Assertions.assertNull(captor.getValue().getCreateByExact());
    }

    @Test
    @DisplayName("mySchedules：按登录教师 teacherId 且 status=0 过滤，透传 semesterId")
    void mySchedules_forcesTeacherAndNormalStatus() throws Exception {
        when(tpmScheduleService.selectTpmScheduleList(any(TpmSchedule.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/adjustment/mySchedules").param("semesterId", "20261"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<TpmSchedule> captor = ArgumentCaptor.forClass(TpmSchedule.class);
        verify(tpmScheduleService).selectTpmScheduleList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getTeacherId());
        Assertions.assertEquals("0", captor.getValue().getStatus());
        Assertions.assertEquals(Long.valueOf(20261L), captor.getValue().getSemesterId());
    }

    @Test
    @DisplayName("add：管理员豁免排课归属，登记口径由登录态覆盖后落库")
    void add_adminBypassesOwnershipAndBindsIdentity() throws Exception {
        TpmScheduleAdjustment saved = new TpmScheduleAdjustment();
        saved.setAdjustId(88L);
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(any()))
                .thenReturn(saved);

        String body = "{\"scheduleId\":10,\"adjustType\":\"1\",\"reason\":\"病假\",\"adjustId\":999,\"approveStatus\":\"2\"}";
        mockMvc.perform(post("/portal/adjustment/apply")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).insertTpmScheduleAdjustment(captor.capture());
        TpmScheduleAdjustment inserted = captor.getValue();
        // 请求体伪造的 adjustId/approveStatus 被登录态覆盖
        Assertions.assertNull(inserted.getAdjustId());
        Assertions.assertEquals("0", inserted.getApproveStatus());
        Assertions.assertEquals("tester", inserted.getCreateBy());
        // 管理员豁免，不应触达排课/开课归属查询
        verify(tpmScheduleService, never()).selectTpmScheduleByScheduleId(any());
    }

    @Test
    @DisplayName("add：普通教师仅可针对本人任课排课发起（开课教师匹配放行）")
    void add_teacherOwnSchedulePasses() throws Exception {
        loginAs(100L);
        TpmSchedule schedule = new TpmSchedule();
        schedule.setScheduleId(10L);
        schedule.setOfferingId(20L);
        when(tpmScheduleService.selectTpmScheduleByScheduleId(10L)).thenReturn(schedule);
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(20L);
        offering.setTeacherId(100L);
        when(tpmCourseOfferingService.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(offering);

        controller.add(validAdjustment());

        verify(tpmScheduleAdjustmentService).insertTpmScheduleAdjustment(any(TpmScheduleAdjustment.class));
    }

    @Test
    @DisplayName("add：排课非本人任课被拒绝，不落库")
    void add_rejectsOtherTeacherSchedule() {
        loginAs(100L);
        TpmSchedule schedule = new TpmSchedule();
        schedule.setScheduleId(10L);
        schedule.setOfferingId(20L);
        when(tpmScheduleService.selectTpmScheduleByScheduleId(10L)).thenReturn(schedule);
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(20L);
        offering.setTeacherId(200L); // 他人开课
        when(tpmCourseOfferingService.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(offering);

        ServiceException ex = Assertions.assertThrows(ServiceException.class,
                () -> controller.add(validAdjustment()));
        Assertions.assertTrue(ex.getMessage().contains("只能对本人任课的排课发起调停课申请"));
        verify(tpmScheduleAdjustmentService, never()).insertTpmScheduleAdjustment(any());
    }

    @Test
    @DisplayName("add：调课类型非法被前置拒绝")
    void add_rejectsIllegalAdjustType() {
        TpmScheduleAdjustment a = validAdjustment();
        a.setAdjustType("9");

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.add(a));
        Assertions.assertTrue(ex.getMessage().contains("调课类型不合法"));
        verify(tpmScheduleAdjustmentService, never()).insertTpmScheduleAdjustment(any());
    }

    @Test
    @DisplayName("add：申请原因为空白被前置拒绝")
    void add_rejectsBlankReason() {
        TpmScheduleAdjustment a = validAdjustment();
        a.setReason("   ");

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.add(a));
        Assertions.assertTrue(ex.getMessage().contains("申请原因不能为空"));
    }

    @Test
    @DisplayName("cancel：归属校验（admin 放行）后按本人撤销并返回成败")
    void cancel_checksOwnershipThenCancels() throws Exception {
        TpmScheduleAdjustment owned = new TpmScheduleAdjustment();
        owned.setAdjustId(3L);
        owned.setCreateBy("someoneElse");
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(3L)).thenReturn(owned);
        when(tpmScheduleAdjustmentService.cancelByApplicant(eq(3L), eq("tester"))).thenReturn(1);

        mockMvc.perform(put("/portal/adjustment/cancel/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).cancelByApplicant(eq(3L), eq("tester"));
    }

    @Test
    @DisplayName("cancel（直调）：普通教师撤销他人申请被越权拒绝")
    void cancel_rejectsOtherOwner() {
        loginAs(100L);
        TpmScheduleAdjustment other = new TpmScheduleAdjustment();
        other.setAdjustId(3L);
        other.setCreateBy("someoneElse");
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(3L)).thenReturn(other);

        ServiceException ex = Assertions.assertThrows(ServiceException.class, () -> controller.cancel(3L));
        Assertions.assertTrue(ex.getMessage().contains("无权操作他人的调停课申请"));
        verify(tpmScheduleAdjustmentService, never()).cancelByApplicant(any(), any());
    }

    @Test
    @DisplayName("pendingList：管理员放行并强制 approveStatus=0")
    void pendingList_forcesUnapprovedStatus() throws Exception {
        when(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(any(TpmScheduleAdjustment.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/adjustment/pendingList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmScheduleAdjustment> captor = ArgumentCaptor.forClass(TpmScheduleAdjustment.class);
        verify(tpmScheduleAdjustmentService).selectTpmScheduleAdjustmentList(captor.capture());
        Assertions.assertEquals("0", captor.getValue().getApproveStatus());
    }

    @Test
    @DisplayName("pendingList（直调）：非审批人普通教师被拒绝")
    void pendingList_rejectsNonApprover() {
        loginAs(100L);

        ServiceException ex = Assertions.assertThrows(ServiceException.class,
                () -> controller.pendingList(new TpmScheduleAdjustment()));
        Assertions.assertTrue(ex.getMessage().contains("仅调停课审批人可执行审批操作"));
    }

    @Test
    @DisplayName("approve：管理员通过并透传审批意见")
    void approve_passesComment() throws Exception {
        mockMvc.perform(post("/portal/adjustment/approve/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approveComment\":\"同意\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).approve(eq(3L), eq("同意"));
    }

    @Test
    @DisplayName("approve：无请求体时以 null 意见通过")
    void approve_absentBodyNullComment() throws Exception {
        mockMvc.perform(post("/portal/adjustment/approve/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).approve(eq(3L), isNull());
    }

    @Test
    @DisplayName("reject：管理员驳回并透传审批意见")
    void reject_passesComment() throws Exception {
        mockMvc.perform(post("/portal/adjustment/reject/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approveComment\":\"材料不全\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tpmScheduleAdjustmentService).reject(eq(3L), eq("材料不全"));
    }

    private TpmScheduleAdjustment validAdjustment() {
        TpmScheduleAdjustment a = new TpmScheduleAdjustment();
        a.setScheduleId(10L);
        a.setAdjustType("1");
        a.setReason("病假");
        return a;
    }
}
