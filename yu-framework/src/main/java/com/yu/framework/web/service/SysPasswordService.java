package com.yu.framework.web.service;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.yu.common.constant.CacheConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.user.UserPasswordNotMatchException;
import com.yu.common.exception.user.UserPasswordRetryLimitExceedException;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.ServletUtils;
import com.yu.common.utils.ip.IpUtils;
import com.yu.framework.security.context.AuthenticationContextHolder;
import com.yu.system.service.ISysUserService;

/**
 * 登录密码方法
 * 
 * @author ruoyi
 */
@Component
public class SysPasswordService
{
    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ISysUserService userService;

    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;

    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    /**
     * 登录账户密码错误次数缓存键名（G2：账号 + 来源 IP 双维度计数，
     * 避免攻击者定向错密码恶意锁定他人账号；仅锁定“该账号从该 IP”的组合）
     * 
     * @param username 用户名
     * @param ip 登录来源 IP
     * @return 缓存键key
     */
    private String getCacheKey(String username, String ip)
    {
        return CacheConstants.PWD_ERR_CNT_KEY + username + ":" + ip;
    }

    /**
     * 取登录来源 IP；非 Web 上下文（单测/异步）降级为 unknown，不阻断验证
     */
    private String getLoginIp()
    {
        try
        {
            return IpUtils.getIpAddr(ServletUtils.getRequest());
        }
        catch (Exception e)
        {
            return "unknown";
        }
    }

    public void validate(SysUser user)
    {
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();
        String cacheKey = getCacheKey(username, getLoginIp());

        Integer retryCount = redisCache.getCacheObject(cacheKey);

        if (retryCount == null)
        {
            retryCount = 0;
        }

        if (retryCount >= Integer.valueOf(maxRetryCount).intValue())
        {
            throw new UserPasswordRetryLimitExceedException(maxRetryCount, lockTime);
        }

        if (!matches(user, password))
        {
            retryCount = retryCount + 1;
            redisCache.setCacheObject(cacheKey, retryCount, lockTime, TimeUnit.MINUTES);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            redisCache.deleteObject(cacheKey);
        }
    }

    public boolean matches(SysUser user, String rawPassword)
    {
        String encodedPassword = user.getPassword();
        if (StringUtils.isEmpty(encodedPassword))
        {
            // 缓存对象可能缺失密码，从数据库重新加载（绕过缓存，避免再次取到password为null的对象）
            SysUser freshUser = userService.selectUserByUserNameWithoutCache(user.getUserName());
            if (freshUser != null)
            {
                encodedPassword = freshUser.getPassword();
            }
        }
        return SecurityUtils.matchesPassword(rawPassword, encodedPassword);
    }

    /**
     * 管理员解锁：清除该账号全部“账号+IP”组合的错次计数（组合键数量受锁定窗口限制，前缀扫描代价可控）
     */
    public void clearLoginRecordCache(String loginName)
    {
        Collection<String> keys = redisCache.keys(CacheConstants.PWD_ERR_CNT_KEY + loginName + ":*");
        if (keys != null && !keys.isEmpty())
        {
            redisCache.deleteObject(keys);
        }
    }
}
