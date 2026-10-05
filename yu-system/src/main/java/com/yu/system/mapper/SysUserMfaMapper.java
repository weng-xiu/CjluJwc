package com.yu.system.mapper;

import com.yu.system.domain.SysUserMfa;

/**
 * 用户 MFA 绑定 数据层
 *
 * @author cjlu
 */
public interface SysUserMfaMapper
{
    /**
     * 按用户ID查询 MFA 绑定信息。
     *
     * @param userId 用户ID
     * @return 绑定记录，未绑定返回 {@code null}
     */
    public SysUserMfa selectByUserId(Long userId);

    /**
     * 新增或更新绑定记录（按 user_id 幂等 upsert）。
     *
     * @param sysUserMfa 绑定记录
     * @return 影响行数
     */
    public int insertOrUpdate(SysUserMfa sysUserMfa);

    /**
     * 删除用户的 MFA 绑定。
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    public int deleteByUserId(Long userId);
}
