package com.yu.system.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 用户多因子鉴别（MFA）绑定对象 sys_user_mfa
 *
 * <p>K3 MFA 后端地基：为每个用户保存 TOTP 密钥与启用状态。与 sys_user 解耦为独立表，
 * 不污染既有用户主档；仅当 {@code status=1} 时登录流程才会强制二次校验。</p>
 *
 * @author cjlu
 */
public class SysUserMfa extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 待确认（已生成密钥、尚未通过一次性口令验证启用） */
    public static final String STATUS_PENDING = "0";

    /** 已启用（登录时强制 TOTP 二次校验） */
    public static final String STATUS_ENABLED = "1";

    /** 用户ID（主键） */
    private Long userId;

    /** Base32 编码的 TOTP 密钥 */
    private String secret;

    /** 状态（0待确认 1已启用） */
    private String status;

    /** 绑定完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date bindTime;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getBindTime()
    {
        return bindTime;
    }

    public void setBindTime(Date bindTime)
    {
        this.bindTime = bindTime;
    }
}
