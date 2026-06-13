package com.yu.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 用户信息缓存（按ID）
     */
    public static final String SYS_USER_ID_KEY = "sys_user:id:";

    /**
     * 用户信息缓存（按用户名）
     */
    public static final String SYS_USER_NAME_KEY = "sys_user:name:";

    /**
     * 用户缓存过期时间（分钟）
     */
    public static final int SYS_USER_CACHE_EXPIRATION = 30;

    /**
     * 系统配置缓存过期时间（分钟）
     */
    public static final int SYS_CONFIG_CACHE_EXPIRATION = 120;
}
