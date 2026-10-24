package com.yu.web.controller.system;

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
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.yu.system.domain.SysUserMfa;
import com.yu.system.service.ISysUserMfaService;

/**
 * 多因子鉴别（MFA / TOTP）契约测试（G5 长尾控制器补测）。
 *
 * <p>K3-2 后端地基：状态查询三态（未绑定/待确认/已启用）、绑定发起回传 secret 与 otpauth URI、
 * 确认与解绑的空口令早退与口令校验分流、二维码未发起绑定降级。
 * 登录态由 standalone 注入 SecurityContext 的 LoginUser 提供，
 * TotpUtils.verify 属于服务层职责，此处只锁定控制器契约。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysMfaControllerMockMvcTest
{
    @Mock
    private ISysUserMfaService mfaService;

    @InjectMocks
    private SysMfaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(7L);
        sysUser.setUserName("alice");
        LoginUser loginUser = new LoginUser(7L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("status：未绑定 → enabled=false pending=false bindTime=null")
    void statusNotBound() throws Exception
    {
        when(mfaService.getByUserId(eq(7L))).thenReturn(null);

        mockMvc.perform(get("/mfa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.pending").value(false))
                .andExpect(jsonPath("$.bindTime").doesNotExist());
    }

    @Test
    @DisplayName("status：待确认 → pending=true enabled=false")
    void statusPending() throws Exception
    {
        SysUserMfa mfa = new SysUserMfa();
        mfa.setUserId(7L);
        mfa.setStatus(SysUserMfa.STATUS_PENDING);
        when(mfaService.getByUserId(eq(7L))).thenReturn(mfa);

        mockMvc.perform(get("/mfa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.pending").value(true));
    }

    @Test
    @DisplayName("status：已启用 → enabled=true 且回传 bindTime")
    void statusEnabled() throws Exception
    {
        SysUserMfa mfa = new SysUserMfa();
        mfa.setUserId(7L);
        mfa.setStatus(SysUserMfa.STATUS_ENABLED);
        mfa.setBindTime(new Date());
        when(mfaService.getByUserId(eq(7L))).thenReturn(mfa);

        mockMvc.perform(get("/mfa/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.pending").value(false))
                .andExpect(jsonPath("$.bindTime").exists());
    }

    @Test
    @DisplayName("bind：调用 startBind 并回传 secret 与 otpauth:// URI")
    void bindReturnsSecretAndUri() throws Exception
    {
        when(mfaService.startBind(eq(7L))).thenReturn("JBSWY3DPEHPK3PXP");

        mockMvc.perform(post("/mfa/bind"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.secret").value("JBSWY3DPEHPK3PXP"))
                .andExpect(jsonPath("$.otpauthUri").value(
                        org.hamcrest.Matchers.containsString("otpauth://totp/")));

        verify(mfaService).startBind(eq(7L));
    }

    @Test
    @DisplayName("confirm：空口令早退，不进入服务层校验")
    void confirmBlankCodeRejected() throws Exception
    {
        mockMvc.perform(post("/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(mfaService, never()).confirmBind(anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("confirm：有效口令 → 服务返回 true → MFA 已启用")
    void confirmValidCodeSuccess() throws Exception
    {
        when(mfaService.confirmBind(eq(7L), eq("123456"))).thenReturn(true);

        mockMvc.perform(post("/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("MFA 已启用"));
    }

    @Test
    @DisplayName("confirm：口令错误 → 500 且提示重试")
    void confirmWrongCodeFails() throws Exception
    {
        when(mfaService.confirmBind(eq(7L), eq("000000"))).thenReturn(false);

        mockMvc.perform(post("/mfa/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("unbind：空口令早退；有效口令服务成功 → 解绑成功")
    void unbindBranches() throws Exception
    {
        mockMvc.perform(post("/mfa/unbind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(mfaService, never()).unbind(anyLong(), org.mockito.ArgumentMatchers.anyString());

        when(mfaService.unbind(eq(7L), eq("654321"))).thenReturn(true);
        mockMvc.perform(post("/mfa/unbind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("qrcode：未发起绑定（无 secret）降级为 500 请先发起绑定")
    void qrcodeRequiresPendingSecret() throws Exception
    {
        when(mfaService.getByUserId(eq(7L))).thenReturn(null);

        mockMvc.perform(get("/mfa/qrcode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("请先发起绑定"));
    }
}
