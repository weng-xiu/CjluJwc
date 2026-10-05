package com.yu.system.service;

import com.yu.system.domain.SysUserMfa;

/**
 * 用户多因子鉴别（MFA）服务
 *
 * @author cjlu
 */
public interface ISysUserMfaService
{
    /**
     * 判断用户是否已启用 MFA（登录是否需二次校验的唯一依据）。
     *
     * @param userId 用户ID
     * @return true 表示已启用，登录需校验 TOTP
     */
    public boolean isEnabled(Long userId);

    /**
     * 查询用户 MFA 绑定记录。
     *
     * @param userId 用户ID
     * @return 绑定记录，未绑定返回 {@code null}
     */
    public SysUserMfa getByUserId(Long userId);

    /**
     * 发起绑定：生成随机 TOTP 密钥并以"待确认"状态落库，返回密钥供生成二维码。
     *
     * <p>重复调用会覆盖上一个未确认的密钥。</p>
     *
     * @param userId 用户ID
     * @return Base32 密钥
     */
    public String startBind(Long userId);

    /**
     * 确认绑定：用待确认密钥校验一次性口令，通过则置为"已启用"。
     *
     * @param userId 用户ID
     * @param code   身份验证器 6 位口令
     * @return 是否确认成功
     */
    public boolean confirmBind(Long userId, String code);

    /**
     * 校验一次性口令（针对已启用用户，供登录二次鉴别调用）。
     *
     * @param userId 用户ID
     * @param code   6 位口令
     * @return 是否匹配
     */
    public boolean verifyCode(Long userId, String code);

    /**
     * 解绑：需通过当前口令校验后删除绑定。
     *
     * @param userId 用户ID
     * @param code   6 位口令
     * @return 是否解绑成功
     */
    public boolean unbind(Long userId, String code);
}
