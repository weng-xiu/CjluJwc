package com.yu.common.utils.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import com.yu.common.constant.CacheConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.spring.SpringUtils;

/**
 * 导出件归属登记（S-3）的语义测试。
 *
 * <p>钉住的是安全语义而非实现细节：落盘时登记的是「当前登录用户」，下载时只有同一用户能通过；
 * 无登录上下文（定时任务/异步导出）登记的件对普通账号一律不可下载；Redis 不可用时必须拒绝而不是放行。
 */
class DownloadOwnershipRegistryTest
{
    private static final String FILE_NAME = "6f1c2a3d-export.xlsx";

    private RedisCache redisCache;

    private Object originalBeanFactory;

    @BeforeEach
    void setUp()
    {
        redisCache = Mockito.mock(RedisCache.class);
        // SpringUtils 走静态 beanFactory 取 RedisCache，测试里替换成只含这一个 bean 的伪工厂
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("redisCache", redisCache);
        originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
    }

    @Test
    @DisplayName("落盘时登记当前登录用户为归属人（带过期时间，避免登记永久驻留）")
    void register_writesOwnerOfCurrentUser()
    {
        loginAs(2L);

        DownloadOwnershipRegistry.register(FILE_NAME);

        verify(redisCache).setCacheObject(eq(CacheConstants.DOWNLOAD_OWNER_KEY + FILE_NAME), eq("2"),
                eq(CacheConstants.DOWNLOAD_OWNER_EXPIRATION), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("归属人可下载，其他登录用户不可下载")
    void isOwner_onlyForOwner()
    {
        when(redisCache.getCacheObject(anyString())).thenReturn("2");

        assertTrue(DownloadOwnershipRegistry.isOwner(FILE_NAME, 2L));
        assertFalse(DownloadOwnershipRegistry.isOwner(FILE_NAME, 3L), "横向越权必须被拦住");
    }

    @Test
    @DisplayName("无登录上下文（定时任务/异步导出）登记的件对普通账号不可下载")
    void register_withoutLoginContext_registersSystemOwner()
    {
        SecurityContextHolder.clearContext();

        DownloadOwnershipRegistry.register(FILE_NAME);

        verify(redisCache).setCacheObject(eq(CacheConstants.DOWNLOAD_OWNER_KEY + FILE_NAME),
                eq(DownloadOwnershipRegistry.SYSTEM_OWNER), eq(CacheConstants.DOWNLOAD_OWNER_EXPIRATION),
                eq(TimeUnit.MINUTES));
        when(redisCache.getCacheObject(anyString())).thenReturn(DownloadOwnershipRegistry.SYSTEM_OWNER);
        assertFalse(DownloadOwnershipRegistry.isOwner(FILE_NAME, 2L));
    }

    @Test
    @DisplayName("未登记 / 登记已过期的文件一律拒绝（fail-closed）")
    void isOwner_rejectsUnregisteredFile()
    {
        when(redisCache.getCacheObject(anyString())).thenReturn(null);

        assertFalse(DownloadOwnershipRegistry.isOwner(FILE_NAME, 2L));
    }

    @Test
    @DisplayName("Redis 查询异常时拒绝下载（不能因基础设施抖动放行越权）")
    void isOwner_rejectsWhenCacheUnavailable()
    {
        when(redisCache.getCacheObject(anyString())).thenThrow(new RuntimeException("redis down"));

        assertFalse(DownloadOwnershipRegistry.isOwner(FILE_NAME, 2L));
    }

    @Test
    @DisplayName("登记失败不抛异常打断导出（只是该文件之后只有超管可下载）")
    void register_swallowsCacheFailure()
    {
        loginAs(2L);
        Mockito.doThrow(new RuntimeException("redis down"))
                .when(redisCache).setCacheObject(anyString(), Mockito.any(), Mockito.anyInt(), Mockito.any());

        DownloadOwnershipRegistry.register(FILE_NAME);
    }

    /** 伪造已登录身份：SecurityUtils 从 SecurityContext 取 LoginUser。 */
    private void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        // LoginUser 的 userId 是独立字段，只能由四参构造器写入（SysUser 上的 userId 不会被读取）
        LoginUser loginUser = new LoginUser(userId, null, user, new HashSet<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, new HashSet<>()));
    }
}
