package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.yu.common.constant.CacheConstants;
import com.yu.common.utils.uuid.IdUtils;
import com.yu.framework.web.service.SysPasswordService;

/**
 * G4 首批集成测试 · 登录链路（V5.0 §7.2 G4 / 债务 D11）。
 *
 * <p>走真实容器上下文：Security 过滤器链 + /login 控制器 + UserDetails/密码校验
 * + Redis 令牌签发 + 验证码开关组合。数据只读 sys_user（fixture 账号 admin），
 * 运行期增量仅 Redis 键与 sys_logininfor 日志行，测试各自清理 Redis。
 */
class SysLoginChainIntegrationTest extends AbstractIntegrationTest
{
    @Autowired
    private SysPasswordService passwordService;

    @AfterEach
    void cleanRedisCounters()
    {
        // 清理失败计数，避免污染真实登录锁定状态（G2 双维度键 pwd_err_cnt:{user}:{ip}）
        passwordService.clearLoginRecordCache("admin");
    }

    @Test
    @DisplayName("登录链路：admin 正确口令 → 签发令牌 → 令牌过 JWT 过滤器访问 /getInfo")
    void loginSuccessIssuesTokenAndAccessesGetInfo() throws Exception
    {
        String uuid = IdUtils.fastSimpleUUID();
        // 预置验证码进 Redis：开关开则校验通过，开关关则该键闲置自然过期，两种库配置都能跑
        redisCache.setCacheObject(CacheConstants.CAPTCHA_CODE_KEY + uuid, "1", 2, TimeUnit.MINUTES);

        String body = "{\"username\":\"admin\",\"password\":\"" + FIXTURE_PASSWORD + "\",\"code\":\"1\",\"uuid\":\"" + uuid + "\"}";
        String response = mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(response, "$.token");
        assertNotNull(token, "登录成功应签发 token");

        // 携带令牌穿过完整 Security 过滤器链（JwtAuthenticationTokenFilter → Redis 会话）
        mockMvc.perform(get("/getInfo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.user.userName").value("admin"));

        // 令牌会话应已写入 Redis（真实 TokenService 装配）
        assertNotNull(redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*"), "登录令牌会话键可见");
    }

    @Test
    @DisplayName("登录链路：错误口令 → code=500 且 Redis 计入『账号+IP』失败键（G2 双维度）")
    void loginWrongPasswordRecordsRetryCounter() throws Exception
    {
        String uuid = IdUtils.fastSimpleUUID();
        redisCache.setCacheObject(CacheConstants.CAPTCHA_CODE_KEY + uuid, "1", 2, TimeUnit.MINUTES);

        // 错误口令须落在 loginPreCheck 允许的 5-20 字符区间，才能真正进入密码校验分支计数
        String body = "{\"username\":\"admin\",\"password\":\"Wrong-Pass-" + IdUtils.fastSimpleUUID().substring(0, 4)
                + "\",\"code\":\"1\",\"uuid\":\"" + uuid + "\"}";
        mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        Collection<String> keys = redisCache.keys(CacheConstants.PWD_ERR_CNT_KEY + "admin:*");
        assertNotNull(keys);
        assertFalse(keys.isEmpty(), "错误口令应写入 账号+IP 双维度失败计数键");
        assertEquals(1, ((Number) redisCache.getCacheObject(keys.iterator().next())).intValue(), "首次失败计数应为 1");
    }
}
