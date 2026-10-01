package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.model.RegisterBody;
import com.yu.framework.web.service.SysRegisterService;
import com.yu.system.service.ISysConfigService;

/**
 * 注册接口层测试（Q1 第十二批：MockMvc standalone）。
 * 校验注册开关（配置项 sys.account.registerUser 非 "true" 时直接拒绝且不触达注册服务）、
 * 开启后注册服务返回空串映射为 success、返回错误文案时以该文案降级为 500；
 * 用户名/密码校验、验证码、密码加密与落库等真实逻辑由 SysRegisterService 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysRegisterControllerMockMvcTest {

    @Mock
    private SysRegisterService registerService;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private SysRegisterController controller;

    private MockMvc mockMvc;

    private void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("register：注册开关关闭直接返回 500 且不触达注册服务")
    void register_disabledRejected() throws Exception {
        setup();
        when(configService.selectConfigByKey(eq("sys.account.registerUser"))).thenReturn("false");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("当前系统没有开启注册功能！"));

        verify(registerService, never()).register(any(RegisterBody.class));
    }

    @Test
    @DisplayName("register：开关开启且注册服务返回空串映射为操作成功")
    void register_enabledSuccess() throws Exception {
        setup();
        when(configService.selectConfigByKey(eq("sys.account.registerUser"))).thenReturn("true");
        when(registerService.register(any(RegisterBody.class))).thenReturn("");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"));
    }

    @Test
    @DisplayName("register：注册服务返回错误文案时以该文案降级为 500")
    void register_enabledFailReturnsMessage() throws Exception {
        setup();
        when(configService.selectConfigByKey(eq("sys.account.registerUser"))).thenReturn("true");
        when(registerService.register(any(RegisterBody.class))).thenReturn("注册失败，登录账号已存在");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dup\",\"password\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("注册失败，登录账号已存在"));
    }
}
