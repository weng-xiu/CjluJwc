package com.yu.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yu.common.constant.CacheConstants;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.user.CaptchaException;
import com.yu.common.exception.user.CaptchaExpireException;
import com.yu.common.utils.StringUtils;

/**
 * 验证码校验组件
 * 统一处理登录和注册场景下的验证码逻辑
 *
 * @author ruoyi
 */
@Component
public class CaptchaValidator
{
    @Autowired
    private RedisCache redisCache;

    /**
     * 校验验证码
     *
     * @param username 用户名（用于日志追踪）
     * @param code     用户输入的验证码
     * @param uuid     验证码唯一标识
     * @throws CaptchaExpireException 验证码已过期或不存在时抛出
     * @throws CaptchaException       验证码不匹配时抛出
     */
    public void validate(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
