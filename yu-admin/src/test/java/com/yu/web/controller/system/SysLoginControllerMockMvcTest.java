package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import com.yu.common.core.domain.entity.SysMenu;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.framework.web.service.SysLoginService;
import com.yu.framework.web.service.SysPermissionService;
import com.yu.framework.web.service.TokenService;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysMenuService;

/**
 * 登录验证接口层测试（Q1 第十一批：MockMvc standalone）。
 * 校验 login 委派 SysLoginService 生成令牌并置于 token 字段（不触碰真实认证链），
 * getInfo 聚合用户/角色/权限、当权限集合与登录缓存不一致时刷新令牌、并从配置项派生
 * pwdChrtype（默认 "0"）与两个密码到期布尔位，
 * getRouters 按当前用户 ID 拉取菜单树并构建路由；
 * 用户名密码校验、验证码、JWT 签发等真实逻辑由框架层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysLoginControllerMockMvcTest {

    @Mock
    private SysLoginService loginService;

    @Mock
    private ISysMenuService menuService;

    @Mock
    private SysPermissionService permissionService;

    @Mock
    private TokenService tokenService;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private SysLoginController controller;

    private MockMvc mockMvc;
    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setUserName("tester");
        loginUser = new LoginUser(1L, 2L, sysUser, new HashSet<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("login：委派登录服务生成令牌并写入 token 字段")
    void login_returnsToken() throws Exception {
        when(loginService.login(any(), any(), any(), any())).thenReturn("TOKEN-ABC-123");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.token").value("TOKEN-ABC-123"));

        verify(loginService).login(eq("admin"), eq("admin123"), any(), any());
    }

    @Test
    @DisplayName("getInfo：聚合用户/角色/权限，权限变化时刷新令牌并派生密码配置")
    void getInfo_aggregatesAndRefreshes() throws Exception {
        when(permissionService.getRolePermission(any(SysUser.class)))
                .thenReturn(new HashSet<>(List.of("admin")));
        when(permissionService.getMenuPermission(any(SysUser.class)))
                .thenReturn(new HashSet<>(List.of("system:user:list")));
        when(configService.selectConfigByKey(any(String.class))).thenReturn(null);

        mockMvc.perform(get("/getInfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.user.userName").value("tester"))
                .andExpect(jsonPath("$.roles[0]").value("admin"))
                .andExpect(jsonPath("$.permissions[0]").value("system:user:list"))
                .andExpect(jsonPath("$.pwdChrtype").value("0"))
                .andExpect(jsonPath("$.isDefaultModifyPwd").value(false))
                .andExpect(jsonPath("$.isPasswordExpired").value(false));

        verify(tokenService).refreshToken(loginUser);
    }

    @Test
    @DisplayName("getInfo：权限集合与缓存一致时不刷新令牌")
    void getInfo_permissionsUnchangedNoRefresh() throws Exception {
        when(permissionService.getRolePermission(any(SysUser.class))).thenReturn(new HashSet<>());
        when(permissionService.getMenuPermission(any(SysUser.class))).thenReturn(new HashSet<>());
        when(configService.selectConfigByKey(any(String.class))).thenReturn(null);

        mockMvc.perform(get("/getInfo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tokenService, never()).refreshToken(loginUser);
    }

    @Test
    @DisplayName("getRouters：按当前用户 ID 拉取菜单树并构建路由")
    void getRouters_buildsMenuTree() throws Exception {
        when(menuService.selectMenuTreeByUserId(eq(1L))).thenReturn(List.of(new SysMenu()));
        when(menuService.buildMenus(any(List.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/getRouters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(menuService).selectMenuTreeByUserId(eq(1L));
    }
}
