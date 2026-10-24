package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

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

import com.yu.common.config.RuoYiConfig;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.utils.SecurityUtils;
import com.yu.system.service.ISysUserService;

/**
 * 首页控制器契约测试（G5 长尾控制器补测）。
 *
 * <p>覆盖两条契约：
 * <ul>
 *   <li>GET "/" 输出欢迎语（含配置化 name/version），仅前端不可用时的兜底提示</li>
 *   <li>POST "/unlockscreen" 屏幕解锁四分支：空密码 → 用户缺失 → 密码错（BCrypt 匹配）→ 成功；
 *       解锁属于二次鉴权场景，任何一步失败均不查库执行更新</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysIndexControllerMockMvcTest
{
    @Mock
    private RuoYiConfig ruoyiConfig;

    @Mock
    private ISysUserService userService;

    @InjectMocks
    private SysIndexController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(11L);
        sysUser.setUserName("carol");
        LoginUser loginUser = new LoginUser(11L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /：欢迎语含配置的 name/version，作为无前端时兜底")
    void indexWelcomeText() throws Exception
    {
        when(ruoyiConfig.getName()).thenReturn("CJLU-JWC");
        when(ruoyiConfig.getVersion()).thenReturn("3.9.2");

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("CJLU-JWC")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("v3.9.2")));
    }

    @Test
    @DisplayName("unlockScreen：空密码早退，不查库")
    void unlockEmptyPassword() throws Exception
    {
        mockMvc.perform(post("/unlockscreen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(userService, never()).selectUserByUserName(anyString());
    }

    @Test
    @DisplayName("unlockScreen：用户不存在（会话过期）→ 提示重新登录且不比对密码")
    void unlockUserMissing() throws Exception
    {
        when(userService.selectUserByUserName(eq("carol"))).thenReturn(null);

        mockMvc.perform(post("/unlockscreen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("服务器超时，请重新登录"));
    }

    @Test
    @DisplayName("unlockScreen：BCrypt 密码不匹配 → 拒绝解锁")
    void unlockPasswordMismatch() throws Exception
    {
        SysUser stored = new SysUser();
        stored.setUserId(11L);
        stored.setUserName("carol");
        stored.setPassword(SecurityUtils.encryptPassword("right-pwd"));
        when(userService.selectUserByUserName(eq("carol"))).thenReturn(stored);

        mockMvc.perform(post("/unlockscreen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong-pwd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("密码错误，请重新输入"));
    }

    @Test
    @DisplayName("unlockScreen：密码匹配 → 解锁成功")
    void unlockSuccess() throws Exception
    {
        SysUser stored = new SysUser();
        stored.setUserId(11L);
        stored.setUserName("carol");
        stored.setPassword(SecurityUtils.encryptPassword("p@ssw0rd"));
        when(userService.selectUserByUserName(eq("carol"))).thenReturn(stored);

        mockMvc.perform(post("/unlockscreen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"p@ssw0rd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("解锁成功"));
    }
}
