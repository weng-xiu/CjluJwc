package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.exception.ServiceException;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;
import com.yu.system.domain.SysPrintRecord;
import com.yu.system.service.ISysPrintService;

/**
 * 门户打印与电子凭证接口层测试（Q1 第四批：MockMvc standalone）。
 * 校验 P4 防越权契约：myCertificates 强制登录学生（无映射 studentId 兜底 -1）、
 * myRecords 强制 receiveId、checkSelfType 白名单（非白名单抛 ServiceException）、
 * print 归属校验（不存在/非白名单类型/越权 receiveId 分别拦截）；
 * render/issue 透传 enforceOwner=true。渲染/发放内部逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalCredentialControllerMockMvcTest {

    @Mock
    private ISysPrintService sysPrintService;

    @Mock
    private ISamCertificateService samCertificateService;

    @InjectMocks
    private PortalCredentialController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
    @DisplayName("myCertificates：按登录用户映射到 studentId 过滤")
    void myCertificates_bindsMappedStudentId() throws Exception {
        when(sysPrintService.selectStudentIdByUserId(1L)).thenReturn(5L);
        when(samCertificateService.selectSamCertificateList(any(SamCertificate.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/credential/myCertificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SamCertificate> captor = ArgumentCaptor.forClass(SamCertificate.class);
        verify(samCertificateService).selectSamCertificateList(captor.capture());
        Assertions.assertEquals(Long.valueOf(5L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("myCertificates：userId 无学生映射时兜底 studentId=-1（返回空集而非全量）")
    void myCertificates_nullMappingFallsBackToNegative() throws Exception {
        when(sysPrintService.selectStudentIdByUserId(1L)).thenReturn(null);
        when(samCertificateService.selectSamCertificateList(any(SamCertificate.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/portal/credential/myCertificates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SamCertificate> captor = ArgumentCaptor.forClass(SamCertificate.class);
        verify(samCertificateService).selectSamCertificateList(captor.capture());
        Assertions.assertEquals(Long.valueOf(-1L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("mySeats：返回登录用户考场座位列表")
    void mySeats_returnsData() throws Exception {
        when(sysPrintService.selectMySeats(1L)).thenReturn(List.of(Map.of("seatNo", "A1")));

        mockMvc.perform(get("/portal/credential/mySeats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].seatNo").value("A1"));
    }

    @Test
    @DisplayName("myInvigilations：返回登录教师监考安排列表")
    void myInvigilations_returnsData() throws Exception {
        when(sysPrintService.selectMyInvigilations(1L)).thenReturn(List.of());

        mockMvc.perform(get("/portal/credential/myInvigilations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("render：白名单类型预览，透传 enforceOwner=true")
    void render_validTypeSuccess() throws Exception {
        when(sysPrintService.render(eq("GRADE"), eq(1L), any(), eq(true))).thenReturn("<html>ok</html>");

        mockMvc.perform(get("/portal/credential/render")
                        .param("bizType", "GRADE").param("bizId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("<html>ok</html>"));
    }

    @Test
    @DisplayName("render：非白名单类型抛 ServiceException（不落到服务层）")
    void render_invalidTypeThrows() {
        Assertions.assertThrows(ServiceException.class,
                () -> controller.render("SECRET", 1L, null));
        verify(sysPrintService, never()).render(any(), anyLong(), any(), anyBoolean());
    }

    @Test
    @DisplayName("issue：白名单类型自助发放，channel 固定为 '1' 且 enforceOwner=true")
    void issue_validTypeSuccess() throws Exception {
        when(sysPrintService.issue(eq("GRADE"), eq(2L), any(), eq("1"), eq(true))).thenReturn("NO-123");

        mockMvc.perform(post("/portal/credential/issue")
                        .param("bizType", "GRADE").param("bizId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("NO-123"));
    }

    @Test
    @DisplayName("issue：非白名单类型抛 ServiceException")
    void issue_invalidTypeThrows() {
        Assertions.assertThrows(ServiceException.class,
                () -> controller.issue("SECRET", 1L, null));
    }

    @Test
    @DisplayName("print：记录不存在返回错误码 500")
    void print_recordNotFound() throws Exception {
        when(sysPrintService.selectRecordById(9L)).thenReturn(null);

        mockMvc.perform(get("/portal/credential/print/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("凭证不存在"));
    }

    @Test
    @DisplayName("print：记录类型不在自助白名单时视为不存在")
    void print_bizTypeNotInWhitelist() throws Exception {
        SysPrintRecord record = new SysPrintRecord();
        record.setBizType("SECRET");
        record.setReceiveId(1L);
        when(sysPrintService.selectRecordById(10L)).thenReturn(record);

        mockMvc.perform(get("/portal/credential/print/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("凭证不存在"));
    }

    @Test
    @DisplayName("print：归属他人（非本人且非管理员）拒绝访问")
    void print_notOwnerRejected() throws Exception {
        SysPrintRecord record = new SysPrintRecord();
        record.setBizType("GRADE");
        record.setReceiveId(2L); // 登录用户为 1L，非本人且 receiveId 非管理员
        when(sysPrintService.selectRecordById(11L)).thenReturn(record);

        mockMvc.perform(get("/portal/credential/print/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("无权访问该凭证"));
    }

    @Test
    @DisplayName("print：本人凭证正常渲染")
    void print_ownRecordSuccess() throws Exception {
        SysPrintRecord record = new SysPrintRecord();
        record.setBizType("GRADE");
        record.setReceiveId(1L); // 与登录用户一致
        when(sysPrintService.selectRecordById(12L)).thenReturn(record);
        when(sysPrintService.renderByRecord(12L)).thenReturn("<html>own</html>");

        mockMvc.perform(get("/portal/credential/print/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("<html>own</html>"));
    }

    @Test
    @DisplayName("myRecords：查询参数 receiveId 被强制覆盖为登录用户")
    void myRecords_forcesReceiveId() throws Exception {
        when(sysPrintService.selectRecordList(any(SysPrintRecord.class))).thenReturn(List.of());

        mockMvc.perform(get("/portal/credential/myRecords").param("receiveId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<SysPrintRecord> captor = ArgumentCaptor.forClass(SysPrintRecord.class);
        verify(sysPrintService).selectRecordList(captor.capture());
        Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getReceiveId());
    }
}
