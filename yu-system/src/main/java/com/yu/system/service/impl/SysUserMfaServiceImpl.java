package com.yu.system.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.TotpUtils;
import com.yu.system.domain.SysUserMfa;
import com.yu.system.mapper.SysUserMfaMapper;
import com.yu.system.service.ISysUserMfaService;

/**
 * 用户多因子鉴别（MFA）服务实现
 *
 * @author cjlu
 */
@Service
public class SysUserMfaServiceImpl implements ISysUserMfaService
{
    @Autowired
    private SysUserMfaMapper sysUserMfaMapper;

    @Override
    public boolean isEnabled(Long userId)
    {
        if (userId == null)
        {
            return false;
        }
        SysUserMfa mfa = sysUserMfaMapper.selectByUserId(userId);
        return mfa != null && SysUserMfa.STATUS_ENABLED.equals(mfa.getStatus());
    }

    @Override
    public SysUserMfa getByUserId(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        return sysUserMfaMapper.selectByUserId(userId);
    }

    @Override
    public String startBind(Long userId)
    {
        String secret = TotpUtils.generateSecret();
        SysUserMfa mfa = new SysUserMfa();
        mfa.setUserId(userId);
        mfa.setSecret(secret);
        mfa.setStatus(SysUserMfa.STATUS_PENDING);
        mfa.setCreateTime(DateUtils.getNowDate());
        sysUserMfaMapper.insertOrUpdate(mfa);
        return secret;
    }

    @Override
    public boolean confirmBind(Long userId, String code)
    {
        SysUserMfa mfa = sysUserMfaMapper.selectByUserId(userId);
        if (mfa == null || mfa.getSecret() == null)
        {
            return false;
        }
        if (!TotpUtils.verify(mfa.getSecret(), code))
        {
            return false;
        }
        mfa.setStatus(SysUserMfa.STATUS_ENABLED);
        mfa.setBindTime(DateUtils.getNowDate());
        mfa.setUpdateTime(DateUtils.getNowDate());
        sysUserMfaMapper.insertOrUpdate(mfa);
        return true;
    }

    @Override
    public boolean verifyCode(Long userId, String code)
    {
        SysUserMfa mfa = sysUserMfaMapper.selectByUserId(userId);
        if (mfa == null || !SysUserMfa.STATUS_ENABLED.equals(mfa.getStatus()))
        {
            return false;
        }
        return TotpUtils.verify(mfa.getSecret(), code);
    }

    @Override
    public boolean unbind(Long userId, String code)
    {
        SysUserMfa mfa = sysUserMfaMapper.selectByUserId(userId);
        if (mfa == null)
        {
            return false;
        }
        // 解绑须证明持有当前验证器；已启用用严格校验，待确认则允许直接撤销
        if (SysUserMfa.STATUS_ENABLED.equals(mfa.getStatus()) && !TotpUtils.verify(mfa.getSecret(), code))
        {
            return false;
        }
        sysUserMfaMapper.deleteByUserId(userId);
        return true;
    }
}
