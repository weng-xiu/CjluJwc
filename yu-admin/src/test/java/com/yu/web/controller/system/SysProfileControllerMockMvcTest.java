package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.utils.SecurityUtils;
import com.yu.framework.web.service.TokenService;
import com.yu.system.service.ISysUserService;

/**
 * 个人中心接口层测试（Q1 第十一批：MockMvc standalone）。
 * 校验 profile 聚合角色组/岗位组、updateProfile 手机/邮箱唯一性拦截顺序（手机优先于邮箱）与影响行数降级、
 * 成功修改后回写 SecurityContext 缓存（tokenService.setLoginUser）、
 * updatePwd 基于 BCrypt 的旧密码校验、新旧密码相同拒绝、成功重置后刷新缓存，
 * avatar 空文件直接降级为错误（不落盘）；
 * 登录态由 standalone 注入 SecurityContext 的 LoginUser 提供，用户数据更新落库逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysProfileControllerMockMvcTest {

    @Mock
    private ISysUserService userService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private SysProfileController controller;

    private MockMvc mockMvc;
    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        sysUser.setNickName("测试员");
        loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("profile：返回当前用户并聚合角色组/岗位组")
    void profile_aggregatesRoleAndPostGroup() throws Exception {
        when(userService.selectUserRoleGroup(eq("tester"))).thenReturn("管理员");
        when(userService.selectUserPostGroup(eq("tester"))).thenReturn("部门经理");

        mockMvc.perform(get("/system/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userName").value("tester"))
                .andExpect(jsonPath("$.roleGroup").value("管理员"))
                .andExpect(jsonPath("$.postGroup").value("部门经理"));
    }

    @Test
    @DisplayName("updateProfile：手机号码已存在返回 500，优先于邮箱校验且不落库")
    void updateProfile_phoneOccupiedRejected() throws Exception {
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(false);

        mockMvc.perform(put("/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"张三\",\"phonenumber\":\"13800000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改用户'tester'失败，手机号码已存在"));

        verify(userService, never()).updateUserProfile(any(SysUser.class));
    }

    @Test
    @DisplayName("updateProfile：手机为空跳过手机校验，邮箱已存在返回 500")
    void updateProfile_emailOccupiedRejected() throws Exception {
        when(userService.checkEmailUnique(any(SysUser.class))).thenReturn(false);

        mockMvc.perform(put("/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"张三\",\"email\":\"dup@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改用户'tester'失败，邮箱账号已存在"));

        verify(userService, never()).checkPhoneUnique(any(SysUser.class));
        verify(userService, never()).updateUserProfile(any(SysUser.class));
    }

    @Test
    @DisplayName("updateProfile：合法修改回填字段并成功后刷新缓存用户")
    void updateProfile_validUpdatesCache() throws Exception {
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(true);
        when(userService.checkEmailUnique(any(SysUser.class))).thenReturn(true);
        when(userService.updateUserProfile(any(SysUser.class))).thenReturn(1);

        mockMvc.perform(put("/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"李四\",\"email\":\"ok@example.com\",\"sex\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).updateUserProfile(any(SysUser.class));
        verify(tokenService).setLoginUser(loginUser);
    }

    @Test
    @DisplayName("updateProfile：影响行数为 0 返回异常提示")
    void updateProfile_zeroRowsReturnsError() throws Exception {
        when(userService.updateUserProfile(any(SysUser.class))).thenReturn(0);

        mockMvc.perform(put("/system/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickName\":\"李四\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改个人信息异常，请联系管理员"));
    }

    @Test
    @DisplayName("updatePwd：旧密码错误返回 500 且不重置")
    void updatePwd_oldPasswordWrong() throws Exception {
        SysUser stored = new SysUser();
        stored.setUserId(1L);
        stored.setPassword(SecurityUtils.encryptPassword("123456"));
        when(userService.selectUserById(eq(1L))).thenReturn(stored);

        mockMvc.perform(put("/system/user/profile/updatePwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改密码失败，旧密码错误"));

        verify(userService, never()).resetUserPwd(any(Long.class), any(String.class));
    }

    @Test
    @DisplayName("updatePwd：新密码与旧密码相同返回 500")
    void updatePwd_sameAsOldRejected() throws Exception {
        SysUser stored = new SysUser();
        stored.setUserId(1L);
        stored.setPassword(SecurityUtils.encryptPassword("123456"));
        when(userService.selectUserById(eq(1L))).thenReturn(stored);

        mockMvc.perform(put("/system/user/profile/updatePwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新密码不能与旧密码相同"));

        verify(userService, never()).resetUserPwd(any(Long.class), any(String.class));
    }

    @Test
    @DisplayName("updatePwd：旧密码正确且新密码不同则重置并刷新缓存")
    void updatePwd_validResetsAndCaches() throws Exception {
        SysUser stored = new SysUser();
        stored.setUserId(1L);
        stored.setPassword(SecurityUtils.encryptPassword("123456"));
        when(userService.selectUserById(eq(1L))).thenReturn(stored);
        when(userService.resetUserPwd(eq(1L), any(String.class))).thenReturn(1);

        mockMvc.perform(put("/system/user/profile/updatePwd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).resetUserPwd(eq(1L), any(String.class));
        verify(tokenService).setLoginUser(loginUser);
    }

    @Test
    @DisplayName("avatar：空上传文件不落盘直接返回错误")
    void avatar_emptyFileRejected() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("avatarfile", "x.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/system/user/profile/avatar").file(empty))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("上传图片异常，请联系管理员"));

        verify(userService, never()).updateUserAvatar(any(Long.class), any(String.class));
    }
}
