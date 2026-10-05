package com.yu.framework.web.service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.Constants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.ServiceException;
import com.yu.common.exception.user.BlackListException;
import com.yu.common.exception.user.CaptchaException;
import com.yu.common.exception.user.CaptchaExpireException;
import com.yu.common.exception.user.UserNotExistsException;
import com.yu.common.exception.user.UserPasswordNotMatchException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.MessageUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.ip.IpUtils;
import com.yu.framework.manager.AsyncManager;
import com.yu.framework.manager.factory.AsyncFactory;
import com.yu.framework.security.context.AuthenticationContextHolder;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysUserMfaService;
import com.yu.system.service.ISysUserService;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Autowired
    private RedisCache redisCache;
    
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private CaptchaValidator captchaValidator;

    @Autowired
    private ISysUserMfaService mfaService;

    /**
     * 登录验证
     * 
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid)
    {
        return login(username, password, code, uuid, null);
    }

    /**
     * 登录验证（含 K3 MFA 二次鉴别）
     *
     * @param username 用户名
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @param totpCode MFA 一次性口令（仅当用户已启用 MFA 时必填）
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid, String totpCode)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // K3 MFA 二次鉴别：仅对已启用多因子的用户强制校验 TOTP，未启用者登录行为不变
        verifyMfa(loginUser, username, totpCode);
        // 登录成功后清除密码错误计数及用户缓存，防止陈旧缓存干扰下次登录
        redisCache.deleteObject(CacheConstants.PWD_ERR_CNT_KEY + username);
        redisCache.deleteObject(CacheConstants.SYS_USER_NAME_KEY + username);
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * K3 MFA 二次鉴别。仅当用户已启用多因子时强制校验 TOTP 口令；
     * 未启用者直接返回，保持存量登录行为不变。
     *
     * @param loginUser 已通过第一因子（密码）认证的登录用户
     * @param username  用户名（用于登录审计）
     * @param totpCode  用户提交的一次性口令
     */
    private void verifyMfa(LoginUser loginUser, String username, String totpCode)
    {
        if (loginUser == null || loginUser.getUserId() == null)
        {
            return;
        }
        if (!mfaService.isEnabled(loginUser.getUserId()))
        {
            return;
        }
        if (StringUtils.isBlank(totpCode) || !mfaService.verifyCode(loginUser.getUserId(), totpCode.trim()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, "MFA 校验失败"));
            throw new ServiceException("MFA 校验失败：一次性口令不正确或已过期");
        }
    }

    /**
     * 校验验证码
     * 
     * @param username 用户名
     * @param code 验证码
     * @param uuid 唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            try
            {
                captchaValidator.validate(username, code, uuid);
            }
            catch (CaptchaExpireException e)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw e;
            }
            catch (CaptchaException e)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw e;
            }
        }
    }

    /**
     * 登录前置校验
     * @param username 用户名
     * @param password 用户密码
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param userId 用户ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }
}
