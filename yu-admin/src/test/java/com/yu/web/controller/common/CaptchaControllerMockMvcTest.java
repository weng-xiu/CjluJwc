package com.yu.web.controller.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.google.code.kaptcha.Producer;
import com.yu.common.config.RuoYiConfig;
import com.yu.common.core.redis.RedisCache;
import com.yu.system.service.ISysConfigService;

/**
 * 验证码控制器契约测试（G5 长尾控制器补测）。
 *
 * <p>关闭态短路：不生成 uuid/img，也不写 Redis（避免降级时白白消耗 CPU 与存储）。
 * 开启态分流：math 型走 captchaProducerMath，char 型走 captchaProducer；
 * 均按 CAPTCHA_CODE_KEY 前缀写 Redis 并回传 uuid + img(base64)。
 * 登录门闸正确性的接口层护栏。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CaptchaControllerMockMvcTest
{
    @Mock
    private Producer captchaProducer;

    @Mock
    private Producer captchaProducerMath;

    @Mock
    private RedisCache redisCache;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private CaptchaController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown()
    {
        // 复位静态配置，避免污染其它测试类
        ReflectionTestUtils.setField(RuoYiConfig.class, "captchaType", null);
    }

    @Test
    @DisplayName("关闭态短路：captchaEnabled=false 时不生成图片也不写 Redis")
    void disabledShortCircuit() throws Exception
    {
        when(configService.selectCaptchaEnabled()).thenReturn(false);

        mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.captchaEnabled").value(false))
                .andExpect(jsonPath("$.uuid").doesNotExist())
                .andExpect(jsonPath("$.img").doesNotExist());

        verify(redisCache, never()).setCacheObject(anyString(), any(), anyInt(), any(TimeUnit.class));
        verify(captchaProducer, never()).createText();
        verify(captchaProducerMath, never()).createText();
    }

    @Test
    @DisplayName("开启态 · math 型：走算术验证码生产者，Redis 存计算结果而非展示串")
    void enabledMathType() throws Exception
    {
        when(configService.selectCaptchaEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(RuoYiConfig.class, "captchaType", "math");
        // capText 格式 "3+5=@8"：capStr="3+5=" code="8"
        when(captchaProducerMath.createText()).thenReturn("3+5=@8");
        when(captchaProducerMath.createImage(anyString())).thenReturn(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.captchaEnabled").value(true))
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.img").exists());

        // 存储的 code 是 "=" 后的答案，不是展示算式
        verify(redisCache).setCacheObject(
                org.mockito.ArgumentMatchers.startsWith("captcha_codes:"),
                eq("8"), anyInt(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("开启态 · char 型：走字符验证码生产者，Redis 存原始字符")
    void enabledCharType() throws Exception
    {
        when(configService.selectCaptchaEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(RuoYiConfig.class, "captchaType", "char");
        when(captchaProducer.createText()).thenReturn("A3B9");
        when(captchaProducer.createImage(anyString())).thenReturn(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(get("/captchaImage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaEnabled").value(true));

        verify(redisCache).setCacheObject(
                org.mockito.ArgumentMatchers.startsWith("captcha_codes:"),
                eq("A3B9"), anyInt(), eq(TimeUnit.MINUTES));
    }
}
