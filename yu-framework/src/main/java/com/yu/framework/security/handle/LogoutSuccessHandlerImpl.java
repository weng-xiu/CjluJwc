package com.yu.framework.security.handle;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import com.alibaba.fastjson2.JSON;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.Constants;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.MessageUtils;
import com.yu.common.utils.ServletUtils;
import com.yu.common.utils.StringUtils;
import com.yu.framework.manager.AsyncManager;
import com.yu.framework.manager.factory.AsyncFactory;
import com.yu.framework.web.service.TokenService;

/**
 * 自定义退出处理类 返回成功
 * 
 * @author ruoyi
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 退出处理
     * 
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            String userName = loginUser.getUsername();
            Long userId = loginUser.getUserId();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 清除用户基本信息缓存及密码错误计数缓存，防止缓存中密码字段缺失导致重新登录失败
            redisCache.deleteObject(CacheConstants.SYS_USER_NAME_KEY + userName);
            if (userId != null)
            {
                redisCache.deleteObject(CacheConstants.SYS_USER_ID_KEY + userId);
            }
            redisCache.deleteObject(CacheConstants.PWD_ERR_CNT_KEY + userName);
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
