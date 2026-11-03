package com.yu.web.controller.common;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.yu.common.config.RuoYiConfig;
import com.yu.common.constant.CacheConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.spring.SpringUtils;
import com.yu.framework.config.ServerConfig;

/**
 * 通用下载接口的归属校验（S-3 余面）：MockMvc standalone。
 *
 * <p>下载目录是全局共享落盘区，文件名不再是「访问凭据」。这里钉住三条线：
 * ① 导出人本人可以下载；② 其他登录用户即使拿到完整文件名也被 403 拦住；
 * ③ 未登记归属的存量文件同样拒绝（fail-closed），只有超管能越过归属校验取件。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommonControllerMockMvcTest
{
    private static final String FILE_NAME = "0f9c1b2e-7a44-4d3f-9c8e-2b1a5d6e7f80_导出报表.xlsx";

    private static final byte[] FILE_CONTENT = "excel-bytes".getBytes(StandardCharsets.UTF_8);

    @Mock
    private ServerConfig serverConfig;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private CommonController controller;

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    private Object originalBeanFactory;

    private Object originalProfile;

    @BeforeEach
    void setUp() throws Exception
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        originalProfile = ReflectionTestUtils.getField(RuoYiConfig.class, "profile");
        ReflectionTestUtils.setField(RuoYiConfig.class, "profile", tempDir.toString());
        Path downloadDir = tempDir.resolve("download");
        Files.createDirectories(downloadDir);
        Files.write(downloadDir.resolve(FILE_NAME), FILE_CONTENT);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("redisCache", redisCache);
        originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);
    }

    @AfterEach
    void tearDown() throws Exception
    {
        SecurityContextHolder.clearContext();
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        ReflectionTestUtils.setField(RuoYiConfig.class, "profile", originalProfile);
        try (var paths = Files.walk(tempDir))
        {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }

    @Test
    @DisplayName("导出人本人下载：放行并返回文件内容")
    void download_ownerAllowed() throws Exception
    {
        loginAs(2L);
        registeredOwner("2");

        mockMvc.perform(get("/common/download").param("fileName", FILE_NAME))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(content().bytes(FILE_CONTENT));
    }

    @Test
    @DisplayName("他人拿到完整文件名也下载不到：403")
    void download_otherUserForbidden() throws Exception
    {
        loginAs(3L);
        registeredOwner("2");

        mockMvc.perform(get("/common/download").param("fileName", FILE_NAME))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登记归属的存量文件：403（fail-closed，不能默认放行）")
    void download_unregisteredFileForbidden() throws Exception
    {
        loginAs(2L);
        registeredOwner(null);

        mockMvc.perform(get("/common/download").param("fileName", FILE_NAME))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("超管可下载他人导出件（运维取件不被归属校验挡住）")
    void download_adminAllowed() throws Exception
    {
        loginAs(1L);
        registeredOwner("2");

        mockMvc.perform(get("/common/download").param("fileName", FILE_NAME))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FILE_CONTENT));
    }

    private void registeredOwner(String owner)
    {
        when(redisCache.getCacheObject(CacheConstants.DOWNLOAD_OWNER_KEY + FILE_NAME)).thenReturn(owner);
    }

    /** 伪造已登录身份：SecurityUtils 从 SecurityContext 取 LoginUser（userId 为独立字段）。 */
    private void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName("user" + userId);
        LoginUser loginUser = new LoginUser(userId, null, user, new HashSet<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, new HashSet<>()));
    }
}
