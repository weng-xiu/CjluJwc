package com.yu.framework.web.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.Constants;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.ServletUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.http.UserAgentUtils;
import com.yu.common.utils.ip.AddressUtils;
import com.yu.common.utils.ip.IpUtils;
import com.yu.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

/**
 * token验证处理
 * 
 * @author ruoyi
 */
@Component
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    // 令牌自定义标识
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌滑动有效期（默认30分钟）：Redis TTL，空闲超过该时长即过期
    @Value("${token.expireTime}")
    private int expireTime;

    // 令牌绝对有效期（默认720分钟=12小时，S-4）：JWT exp 上限，滑动续期不得越过该窗口
    @Value("${token.maxLifetime:720}")
    private int maxLifetime;

    // S-4：当前签名密钥的 kid。留空时由 secret 的摘要自动派生（多实例无需额外配置即可一致）
    @Value("${token.kid:}")
    private String configuredKid;

    // S-4：轮换过渡期的上一代密钥。配置后，用旧密钥签发的令牌在过渡期内仍可通过验签
    @Value("${token.previousSecret:}")
    private String previousSecret;

    // S-4：上一代密钥的 kid。留空时同样由 previousSecret 派生
    @Value("${token.previousKid:}")
    private String configuredPreviousKid;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private Environment environment;

    /** 允许缺省 token.secret 的 profile：仅本地/测试，其余（prod 等）启动即失败，见 {@link #init()}。 */
    private static final Set<String> SECRET_OPTIONAL_PROFILES = Set.of("dev", "it", "test");

    /**
     * K3 jjwt 升级：由配置密钥派生的 HMAC 签名密钥。
     * 新版 jjwt（0.11.x）对 HS512 强制要求密钥 >= 512 位，旧 0.9.1 未校验。为兼容任意长度的
     * TOKEN_SECRET（可能短于 64 字节），启动时用 SHA-512 摘要将 secret 归一为 64 字节密钥，
     * 既保证长度合规，又对同一 secret 确定性可复现（多实例可互验）。
     */
    private javax.crypto.SecretKey signKey;

    /** 当前签名密钥的 kid，写入 JWT 头部 {@code kid}，解析时据此选择验签密钥（S-4）。 */
    private String signKeyId;

    /** 上一代验签密钥（仅验签、不签发），用于密钥轮换过渡期；未配置时为 null（S-4）。 */
    private javax.crypto.SecretKey previousSignKey;

    /** 上一代验签密钥的 kid；未配置或派生失败时为 null（S-4）。 */
    private String previousSignKeyId;

    /**
     * Q4 安全加固：令牌密钥启动校验。
     * 密钥不再明文写入配置文件（application.yml 的 token.secret 改为 ${TOKEN_SECRET:} 环境变量注入）。
     * 若未注入则生成一次性强随机密钥，保证本地/单机可运行；但重启后旧 token 失效、多实例无法互验，
     * 因此生产与多实例部署必须显式设置 TOKEN_SECRET 环境变量。
     */
    @PostConstruct
    public void init()
    {
        if (StringUtils.isEmpty(secret))
        {
            // S-4：非本地/测试 profile 直接 fail-fast。随机密钥在多实例间不一致，会让请求漂移到
            // 另一实例时 401，且重启即全体失效——这类故障放到启动期暴露，比上线后随机 401 便宜得多。
            if (!isSecretOptionalProfile())
            {
                throw new IllegalStateException("未配置 token.secret（TOKEN_SECRET 环境变量）。"
                        + "当前 profile 不允许使用一次性随机密钥，请显式注入 TOKEN_SECRET（建议 openssl rand -base64 64）。");
            }
            secret = generateRandomSecret();
            log.warn("未检测到 token.secret（TOKEN_SECRET 环境变量），已生成一次性随机密钥用于本地运行。"
                    + "生产/多实例部署请务必设置 TOKEN_SECRET，否则重启将使全部令牌失效、多实例无法互验。");
        }
        else if (secret.length() < 32)
        {
            log.warn("token.secret 长度不足 32 字符（当前 {} 个字符），建议使用 openssl rand -base64 64 生成强随机密钥。",
                    secret.length());
        }
        // K3：派生固定长度的 HMAC 签名密钥（SHA-512 摘要 → 64 字节 → HS512）
        this.signKey = deriveSignKey(secret);

        // S-4：kid 默认由 secret 派生，同一 secret 的多实例得到同一个 kid，无需集中配置；
        // 显式配置 token.kid 时以配置为准（便于人工指定轮换顺序）。
        this.signKeyId = StringUtils.isNotEmpty(configuredKid) ? configuredKid.trim() : deriveKid(secret);

        // S-4：配置上一代密钥即开启轮换过渡期——旧令牌仍可验签，新令牌一律用当前密钥签发。
        if (StringUtils.isNotEmpty(previousSecret))
        {
            this.previousSignKey = deriveSignKey(previousSecret);
            this.previousSignKeyId = StringUtils.isNotEmpty(configuredPreviousKid)
                    ? configuredPreviousKid.trim() : deriveKid(previousSecret);
            if (previousSignKeyId.equals(signKeyId))
            {
                // 两代 kid 相同说明 prev 与当前是同一把密钥，轮换配置无效：宁可启动失败也不静默失效
                throw new IllegalStateException("token.previousSecret 与 token.secret 相同（kid=" + signKeyId
                        + "），轮换配置无效；请确认是否漏改上一代密钥。");
            }
            log.warn("已启用令牌密钥轮换过渡：当前 kid={}，同时接受上一代 kid={} 签发的令牌。"
                    + "待旧令牌全部过期后请移除 token.previousSecret。", signKeyId, previousSignKeyId);
        }
        log.info("令牌签名密钥已就绪，kid={}", signKeyId);
    }

    /** 由明文密钥派生固定长度的 HMAC 签名密钥（SHA-512 摘要 → 64 字节 → HS512）。 */
    private static javax.crypto.SecretKey deriveSignKey(String rawSecret)
    {
        try
        {
            byte[] keyBytes = MessageDigest.getInstance("SHA-512").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-512 不可用，无法初始化令牌签名密钥", e);
        }
    }

    /**
     * S-4：由密钥派生 kid（SHA-256 摘要前 12 位十六进制）。
     *
     * <p>kid 只是密钥的公开标识，不泄露密钥本身（无法从摘要反推 64 字节随机密钥）；
     * 用派生的好处是同一 secret 的所有实例自动得到同一个 kid，无需额外的集中配置项。
     */
    private static String deriveKid(String rawSecret)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            // 取前 6 字节（48 位）已足够区分密钥代次；HexFormat 保证字节转十六进制不带符号扩展
            return java.util.HexFormat.of().formatHex(digest, 0, 6);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 不可用，无法派生令牌 kid", e);
        }
    }

    /** 当前 profile 是否属于允许缺省密钥的本地/测试环境（无激活 profile 时按本地处理）。 */
    private boolean isSecretOptionalProfile()
    {
        String[] profiles = environment == null ? new String[0] : environment.getActiveProfiles();
        if (profiles.length == 0)
        {
            return true;
        }
        for (String profile : profiles)
        {
            if (SECRET_OPTIONAL_PROFILES.contains(profile))
            {
                return true;
            }
        }
        return false;
    }

    /** 生成 64 字节（HS512 建议 >= 64 字节）安全随机密钥，Base64 编码。 */
    private String generateRandomSecret()
    {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 获取用户身份信息
     * 
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
            // 同步从在线令牌索引集合中移除，避免遗留失效成员
            redisCache.setRemove(CacheConstants.LOGIN_TOKEN_INDEX_KEY, token);
        }
    }

    /**
     * 创建令牌
     * 
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        // S-4：显式写入 exp。Redis TTL 仍是逐请求的滑动有效期，exp 只做绝对上限——
        // 即使 Redis 中的会话被续期，令牌也不可能无限期有效；续期不会越过该窗口。
        return createToken(claims, new Date(System.currentTimeMillis() + maxLifetime * MILLIS_MINUTE));
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     * 
     * @param loginUser 登录信息
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     * 
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
        // 维护在线令牌索引集合：以 uuid 为成员，供权限刷新按索引遍历，替代 keys 全量扫描
        if (StringUtils.isNotEmpty(loginUser.getToken()))
        {
            redisCache.setAdd(CacheConstants.LOGIN_TOKEN_INDEX_KEY, loginUser.getToken());
        }
    }

    /**
     * 设置用户代理信息
     * 
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从数据声明生成令牌
     *
     * @param claims     数据声明
     * @param expiration 过期时间（写入 exp 声明）
     * @return 令牌
     */
    String createToken(Map<String, Object> claims, Date expiration)
    {
        // K3 jjwt 0.11.x：signWith(SecretKey) 由密钥长度自动选定 HS512
        // S-4：头部写入 kid，轮换期内解析端才能判定该用哪一代密钥验签
        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, signKeyId)
                .setClaims(claims)
                .setExpiration(expiration)
                .signWith(signKey).compact();
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token)
    {
        // K3 jjwt 0.11.x：parserBuilder().build().parseClaimsJws(token)
        // S-4：按头部 kid 选择验签密钥，支持新旧两代密钥并存
        return Jwts.parserBuilder()
                .setSigningKeyResolver(new SigningKeyResolverAdapter()
                {
                    @Override
                    public java.security.Key resolveSigningKey(JwsHeader jwsHeader, Claims claims)
                    {
                        Object kid = jwsHeader.get(JwsHeader.KEY_ID);
                        return resolveVerifyKey(kid == null ? null : kid.toString());
                    }
                })
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * S-4：按 kid 选出验签密钥。
     *
     * <p>无 kid 的旧令牌（轮换上线前签发）回落到当前密钥，避免升级瞬间把在线用户全部踢下线；
     * 未知 kid 直接拒绝——这正是轮换的意义：被淘汰的密钥不再能验签。
     */
    private java.security.Key resolveVerifyKey(String kid)
    {
        if (StringUtils.isEmpty(kid))
        {
            return signKey;
        }
        if (signKeyId.equals(kid))
        {
            return signKey;
        }
        if (previousSignKey != null && previousSignKeyId != null && previousSignKeyId.equals(kid))
        {
            return previousSignKey;
        }
        throw new IllegalArgumentException("未知的令牌密钥标识 kid=" + kid + "，请确认 TOKEN_SECRET/TOKEN_PREVIOUS_SECRET 配置");
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    /**
     * 角色权限变更后，刷新所有持有该角色的在线用户权限
     *
     * @param roleId            变更的角色ID
     * @param permissionService 权限服务
     */
    public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
    {
        // 基于在线令牌索引集合遍历，替代原 keys login_tokens:* 全量扫描（避免在线量大时 Redis 单命令阻塞）
        java.util.Set<String> tokens = redisCache.getCacheSet(CacheConstants.LOGIN_TOKEN_INDEX_KEY);
        if (tokens == null || tokens.isEmpty())
        {
            return;
        }
        for (String uuid : tokens)
        {
            String key = getTokenKey(uuid);
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null)
            {
                // 令牌已过期但索引未清理，顺带剔除失效成员
                redisCache.setRemove(CacheConstants.LOGIN_TOKEN_INDEX_KEY, uuid);
                continue;
            }
            if (loginUser.getUser() == null || loginUser.getUser().isAdmin())
            {
                // 管理员拥有所有权限，跳过
                continue;
            }
            // 判断该用户是否拥有此角色
            boolean hasRole = loginUser.getUser().getRoles() != null
                    && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
            if (!hasRole)
            {
                continue;
            }
            // 刷新权限缓存
            loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
            refreshToken(loginUser);
            log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
        }
    }
}
