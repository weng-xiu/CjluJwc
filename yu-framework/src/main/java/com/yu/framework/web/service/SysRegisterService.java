package com.yu.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yu.common.constant.Constants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.RegisterBody;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.MessageUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.framework.manager.AsyncManager;
import com.yu.framework.manager.factory.AsyncFactory;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysUserService;

/**
 * 注册校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysRegisterService
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private CaptchaValidator captchaValidator;

    /**
     * 注册
     */
    public String register(RegisterBody registerBody)
    {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            captchaValidator.validate(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            throw new ServiceException("用户名不能为空");
        }
        if (StringUtils.isEmpty(password))
        {
            throw new ServiceException("用户密码不能为空");
        }
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            throw new ServiceException("账户长度必须在2到20个字符之间");
        }
        validatePasswordStrength(password);
        if (!userService.checkUserNameUnique(sysUser))
        {
            throw new ServiceException("保存用户'" + username + "'失败，注册账号已存在");
        }
        sysUser.setNickName(username);
        sysUser.setPwdUpdateDate(DateUtils.getNowDate());
        sysUser.setPassword(SecurityUtils.encryptPassword(password));
        boolean regFlag = userService.registerUser(sysUser);
        if (!regFlag)
        {
            throw new ServiceException("注册失败,请联系系统管理人员");
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
        return "";
    }

    /**
     * 校验密码强度
     * 密码必须包含大写字母、小写字母和数字，长度8-20
     *
     * @param password 密码
     */
    private void validatePasswordStrength(String password)
    {
        if (password.length() < 8 || password.length() > 20)
        {
            throw new ServiceException("密码长度必须在8到20个字符之间");
        }
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*\\d.*"))
        {
            throw new ServiceException("密码必须包含大写字母、小写字母和数字");
        }
    }
}
