package com.yu.common.security;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 字段级脱敏集中策略（A4 数据权限体系化）。
 *
 * <p>把"当前请求是否需要脱敏敏感字段"的判定收敛到唯一入口，替代此前散落在
 * 序列化器内联逻辑中的隐式规则，便于审计证明隔离有效性：
 * <ul>
 *   <li>无登录上下文（定时任务/内部调用/异常）—— 一律脱敏，故障安全；</li>
 *   <li>超级管理员 —— 不脱敏；</li>
 *   <li>命中豁免角色（{@link #EXEMPT_ROLE_KEYS}）—— 不脱敏，供未来按业务授权放开；</li>
 *   <li>其余登录用户 —— 脱敏。</li>
 * </ul>
 *
 * <p>该策略为无状态静态工具，因为 Jackson 序列化器实例不由 Spring 容器托管。
 *
 * @author A4
 */
public final class SensitivePolicy
{
    /**
     * 拥有明文可见特权的角色标识集中登记处。默认仅管理员，后续新增豁免只需在此登记，
     * 无需改动序列化链路，保证脱敏口径单一来源。
     */
    private static final Set<String> EXEMPT_ROLE_KEYS = Set.of();

    private SensitivePolicy()
    {
    }

    /**
     * 判定当前上下文是否需要脱敏。
     *
     * @return true 表示应脱敏（隐藏明文），false 表示可见明文
     */
    public static boolean shouldDesensitize()
    {
        LoginUser loginUser = currentLoginUser();
        if (loginUser == null)
        {
            // 故障安全：拿不到身份时按最严格处理
            return true;
        }
        SysUser user = loginUser.getUser();
        if (user == null)
        {
            return true;
        }
        if (user.isAdmin())
        {
            return false;
        }
        return !hasExemptRole(user);
    }

    /** 是否命中集中登记的豁免角色 */
    private static boolean hasExemptRole(SysUser user)
    {
        if (EXEMPT_ROLE_KEYS.isEmpty() || user.getRoles() == null)
        {
            return false;
        }
        for (SysRole role : user.getRoles())
        {
            if (role != null && EXEMPT_ROLE_KEYS.contains(role.getRoleKey()))
            {
                return true;
            }
        }
        return false;
    }

    /** 安全获取当前登录用户，任何异常都返回 null 交由上层走故障安全分支 */
    private static LoginUser currentLoginUser()
    {
        try
        {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null)
            {
                return null;
            }
            Object principal = authentication.getPrincipal();
            return principal instanceof LoginUser ? (LoginUser) principal : null;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
