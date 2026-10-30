package com.yu.framework.web.service;

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
import io.jsonwebtoken.Jwts;
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
        try
        {
            byte[] keyBytes = java.security.MessageDigest.getInstance("SHA-512")
                    .digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            this.signKey = Keys.hmacShaKeyFor(keyBytes);
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-512 不可用，无法初始化令牌签名密钥", e);
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
        return Jwts.builder()
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
        // K3 jjwt 0.11.x：parserBuilder().setSigningKey(SecretKey).build()
        return Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
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
