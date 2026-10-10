package com.yu.web.controller.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.constant.CacheConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.system.domain.SysUserOnline;
import com.yu.system.service.ISysUserOnlineService;

/**
 * 在线用户监控接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验遍历登录令牌缓存键装配在线用户列表（空缓存 / 单会话默认转换分支），
 * 以及强退用户删除对应 LOGIN_TOKEN_KEY 缓存键的契约；
 * 在线用户对象组装与筛选策略由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserOnlineControllerMockMvcTest {

    @Mock
    private ISysUserOnlineService userOnlineService;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private SysUserOnlineController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("list：无在线会话时返回空表格")
    void list_emptyCacheReturnsEmptyTable() throws Exception {
        when(redisCache.keys(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/monitor/online/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.rows").isArray());
    }

    @Test
    @DisplayName("list：单个登录会话默认走 loginUserToUserOnline 转换")
    void list_convertsSingleSession() throws Exception {
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        when(redisCache.keys(anyString())).thenReturn(List.of(CacheConstants.LOGIN_TOKEN_KEY + "abc"));
        when(redisCache.getCacheObject(anyString())).thenReturn(loginUser);
        when(userOnlineService.loginUserToUserOnline(loginUser)).thenReturn(new SysUserOnline());

        mockMvc.perform(get("/monitor/online/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.total").value(1));

        verify(userOnlineService).loginUserToUserOnline(loginUser);
    }

    @Test
    @DisplayName("forceLogout：删除对应登录令牌缓存键")
    void forceLogout_deletesTokenKey() throws Exception {
        mockMvc.perform(delete("/monitor/online/xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(redisCache).deleteObject(CacheConstants.LOGIN_TOKEN_KEY + "xyz");
    }
}
