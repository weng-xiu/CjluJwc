package com.yu.framework.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.Method;
import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import io.jsonwebtoken.Claims;

/**
 * TokenService 的 S-4 安全语义测试。
 *
 * <p>钉住两件事：① 签发令牌必须带 {@code exp} 声明，且窗口等于 {@code token.maxLifetime}
 * 而不是滑动有效期 {@code token.expireTime}（否则 30 分钟后的滑动续期会被 jjwt 直接判过期，
 * 用户被无谓登出）；② 非本地/测试 profile 缺 {@code TOKEN_SECRET} 时必须启动失败。
 */
class TokenServiceTest
{
    private static final String SECRET = "unit-test-secret-0123456789-abcdefghijklmn";

    @AfterEach
    void clearRequestContext()
    {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("签发令牌写入 exp = 当前时间 + maxLifetime（且不是 expireTime）")
    void createToken_writesExpirationBoundToMaxLifetime() throws Exception
    {
        TokenService tokenService = newService(SECRET, "dev", 30, 720);
        LoginUser loginUser = newLoginUser("zhangsan");

        String token = tokenService.createToken(loginUser);

        Claims claims = parse(tokenService, token);
        assertNotNull(claims.getExpiration(), "JWT 必须包含 exp 声明（S-4）");
        long remainingMinutes = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 60000L;
        // 允许 1 分钟的执行抖动，但必须明显大于滑动有效期 30 分钟
        assertTrue(remainingMinutes > 700 && remainingMinutes <= 720,
                "exp 应落在 maxLifetime(720) 窗口内，实际剩余 " + remainingMinutes + " 分钟");
        assertEquals("zhangsan", claims.getSubject(), "用户名仍通过 sub 声明下发");
        assertNotNull(claims.get("login_user_key"), "会话 uuid 声明不得丢失");
    }

    @Test
    @DisplayName("exp 到期的令牌不再解析出登录用户（不再依赖 Redis 才发现失效）")
    void getLoginUser_expiredToken_returnsNull()
    {
        TokenService tokenService = newService(SECRET, "dev", 30, -1);
        String token = tokenService.createToken(newLoginUser("lisi"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        // 签发阶段本身会写 Redis（refreshToken），先清掉再断言"过期判定早于 Redis"
        Object redisCache = ReflectionTestUtils.getField(tokenService, "redisCache");
        clearInvocations(redisCache);

        assertNull(tokenService.getLoginUser(request));
        // 过期判定必须发生在 Redis 查询之前：否则缓存命中的过期令牌仍会被放行
        verifyNoInteractions(redisCache);
    }

    @Test
    @DisplayName("prod profile 缺 TOKEN_SECRET 时启动失败（fail-fast，不再随机生成）")
    void init_prodProfileWithoutSecret_failsFast()
    {
        TokenService tokenService = newUninitializedService("", "prod", 30, 720);

        IllegalStateException ex = assertThrows(IllegalStateException.class, tokenService::init);
        assertTrue(ex.getMessage().contains("TOKEN_SECRET"), "异常信息需指出缺失的配置项，实际=" + ex.getMessage());
    }

    @Test
    @DisplayName("prod profile 已配置 TOKEN_SECRET 时正常启动")
    void init_prodProfileWithSecret_succeeds()
    {
        TokenService tokenService = newService(SECRET, "prod", 30, 720);

        tokenService.init();

        assertEquals(SECRET, ReflectionTestUtils.getField(tokenService, "secret"));
    }

    @Test
    @DisplayName("dev profile 缺 TOKEN_SECRET 仍允许启动（本地运行不被门禁挡住）")
    void init_devProfileWithoutSecret_generatesRandomSecret()
    {
        TokenService tokenService = newService("", "dev", 30, 720);

        tokenService.init();

        Object secret = ReflectionTestUtils.getField(tokenService, "secret");
        assertNotNull(secret);
        assertTrue(secret.toString().length() >= 64, "一次性随机密钥应为 64 字节 Base64");
    }

    // ---------------- S-4：kid 与双密钥轮换 ----------------

    @Test
    @DisplayName("签发令牌在 JWT 头部写入 kid，且 kid 由 secret 确定性派生（多实例一致）")
    void createToken_writesDerivedKidHeader() throws Exception
    {
        TokenService tokenService = newService(SECRET, "dev", 30, 720);
        TokenService anotherInstance = newService(SECRET, "dev", 30, 720);

        String kid = kidOf(tokenService.createToken(newLoginUser("zhangsan")));
        String kidOnAnotherInstance = kidOf(anotherInstance.createToken(newLoginUser("zhangsan")));

        assertNotNull(kid, "JWT 头部必须带 kid，否则轮换期内无法判定用哪代密钥验签");
        assertTrue(kid.matches("[0-9a-f]{12}"), "派生 kid 应为 12 位十六进制，实际=" + kid);
        assertEquals(kid, kidOnAnotherInstance, "同一 TOKEN_SECRET 的所有实例必须得到同一个 kid");
        assertFalse(kid.contains(SECRET), "kid 不得泄露密钥本身");
    }

    @Test
    @DisplayName("显式配置 token.kid 时以其为准（便于人工安排轮换顺序）")
    void createToken_prefersConfiguredKid() throws Exception
    {
        TokenService tokenService = newService(SECRET, "dev", 30, 720, "k-2026-11", null, null);

        assertEquals("k-2026-11", kidOf(tokenService.createToken(newLoginUser("zhangsan"))));
    }

    @Test
    @DisplayName("轮换过渡期：上一代密钥签发的旧令牌仍可验签")
    void parseToken_acceptsTokenSignedByPreviousSecret() throws Exception
    {
        String oldSecret = "old-secret-for-rotation-0123456789-abcdefghij";
        // 轮换前（只有旧密钥）签发的令牌
        TokenService beforeRotation = newService(oldSecret, "dev", 30, 720);
        String oldToken = beforeRotation.createToken(newLoginUser("wangwu"));
        // 轮换后：新密钥签发，但保留上一代密钥用于验签
        TokenService afterRotation = newService(SECRET, "dev", 30, 720, null, oldSecret, null);

        Claims claims = parse(afterRotation, oldToken);

        assertEquals("wangwu", claims.getSubject(), "过渡期内旧令牌不应被立即踢下线");
        String newKid = kidOf(afterRotation.createToken(newLoginUser("wangwu")));
        assertFalse(newKid.equals(kidOf(oldToken)), "新令牌必须用新密钥签发（kid 应不同）");
    }

    @Test
    @DisplayName("未知 kid 的令牌被拒（被淘汰的密钥不再能验签）")
    void parseToken_rejectsUnknownKid() throws Exception
    {
        String strangerSecret = "stranger-secret-0123456789-abcdefghijklmnop";
        String strangerToken = newService(strangerSecret, "dev", 30, 720).createToken(newLoginUser("hacker"));
        TokenService tokenService = newService(SECRET, "dev", 30, 720);

        Exception ex = assertThrows(Exception.class, () -> parse(tokenService, strangerToken));
        assertTrue(rootMessage(ex).contains("未知的令牌密钥标识"), "应明确报未知 kid，实际=" + rootMessage(ex));
    }

    @Test
    @DisplayName("previousSecret 与 secret 相同则启动失败（避免无效轮换静默生效）")
    void init_previousSecretSameAsSecret_failsFast()
    {
        TokenService tokenService = newUninitializedService(SECRET, "dev", 30, 720, null, SECRET, null);

        IllegalStateException ex = assertThrows(IllegalStateException.class, tokenService::init);
        assertTrue(ex.getMessage().contains("previousSecret"), "异常信息需指出配置项，实际=" + ex.getMessage());
    }

    /** 构造并完成 init 的实例：签发/解析令牌需要已就绪的签名密钥。 */
    private TokenService newService(String secret, String profile, int expireTime, int maxLifetime)
    {
        return newService(secret, profile, expireTime, maxLifetime, null, null, null);
    }

    /** S-4：带 kid / previousSecret / previousKid 的实例（轮换相关用例使用）。 */
    private TokenService newService(String secret, String profile, int expireTime, int maxLifetime,
            String kid, String previousSecret, String previousKid)
    {
        TokenService tokenService = newUninitializedService(secret, profile, expireTime, maxLifetime,
                kid, previousSecret, previousKid);
        tokenService.init();
        return tokenService;
    }

    /** 仅装配字段、不跑 init：供「init 自身行为」的用例断言。 */
    private TokenService newUninitializedService(String secret, String profile, int expireTime, int maxLifetime)
    {
        return newUninitializedService(secret, profile, expireTime, maxLifetime, null, null, null);
    }

    /** 仅装配字段、不跑 init（含 S-4 的 kid / 上一代密钥配置）。 */
    private TokenService newUninitializedService(String secret, String profile, int expireTime, int maxLifetime,
            String kid, String previousSecret, String previousKid)
    {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "header", "Authorization");
        ReflectionTestUtils.setField(tokenService, "secret", secret);
        ReflectionTestUtils.setField(tokenService, "expireTime", expireTime);
        ReflectionTestUtils.setField(tokenService, "maxLifetime", maxLifetime);
        ReflectionTestUtils.setField(tokenService, "configuredKid", kid);
        ReflectionTestUtils.setField(tokenService, "previousSecret", previousSecret);
        ReflectionTestUtils.setField(tokenService, "configuredPreviousKid", previousKid);
        ReflectionTestUtils.setField(tokenService, "redisCache", mock(RedisCache.class));
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        ReflectionTestUtils.setField(tokenService, "environment", environment);
        return tokenService;
    }

    /** 取令牌头部的 kid：直接解码 JOSE header（base64url），不依赖业务内部实现。 */
    private String kidOf(String token)
    {
        String headerJson = new String(
                java.util.Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"kid\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(headerJson);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 取异常链最内层的消息：jjwt 会把 resolver 抛出的异常包一层 SignatureException。 */
    private String rootMessage(Throwable ex)
    {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current)
        {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }

    private LoginUser newLoginUser(String userName)
    {
        SysUser user = new SysUser();
        user.setUserName(userName);
        LoginUser loginUser = new LoginUser(user, new HashSet<>());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return loginUser;
    }

    /** 走真实私有解析方法，避免测试里重写一套密钥派生逻辑而失去校验意义。 */
    private Claims parse(TokenService tokenService, String token) throws Exception
    {
        Method parseToken = TokenService.class.getDeclaredMethod("parseToken", String.class);
        parseToken.setAccessible(true);
        return (Claims) parseToken.invoke(tokenService, token);
    }
}
