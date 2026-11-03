package com.yu.portal.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.utils.SecurityUtils;

/**
 * 调停课审批人策略（S-5 门户审批角色化）。
 *
 * <p>此前 {@code PortalAdjustmentController#checkApprover} 只比对单个配置用户名
 * {@code tpm.adjust.approver}，换人必须改配置并重启，且无法与权限体系/角色打通。本策略把审批资格
 * 收敛为一个可注解、可单测的出口，判定顺序：
 * <ol>
 *   <li>超级管理员（userId=1）——兜底；</li>
 *   <li>持有权限字符 {@code portal:adjustment:approve}（角色分配即可，无需重启）；</li>
 *   <li>用户名在 {@code tpm.adjust.approver}（单值，兼容旧配置）或 {@code tpm.adjust.approvers}
 *       （逗号分隔名单）中。</li>
 * </ol>
 *
 * <p>Controller 的 {@code @PreAuthorize} 引用本 bean（{@code @portalAdjustmentApprover.isApprover}），
 * 方法体再经 {@link #canApprove} 做一次纵深校验，避免仅靠注解或仅靠代码内判断。
 *
 * @author S5
 */
@Component("portalAdjustmentApprover")
public class PortalAdjustmentApproverPolicy
{
    /** 审批权限字符（对应 sys_menu 按钮权限，V20261101_1 迁移落库） */
    public static final String APPROVE_PERM = "portal:adjustment:approve";

    private final Set<String> configuredApprovers;

    public PortalAdjustmentApproverPolicy(
            @Value("${tpm.adjust.approver:admin}") String legacyApprover,
            @Value("${tpm.adjust.approvers:}") String approverList)
    {
        Set<String> names = new LinkedHashSet<>();
        addAll(names, legacyApprover);
        addAll(names, approverList);
        this.configuredApprovers = Collections.unmodifiableSet(names);
    }

    private static void addAll(Set<String> target, String raw)
    {
        if (raw == null)
        {
            return;
        }
        for (String item : raw.split(","))
        {
            String name = item.trim();
            if (!name.isEmpty())
            {
                target.add(name);
            }
        }
    }

    /** 用户名是否在审批人配置名单内（供 @PreAuthorize SpEL 使用） */
    public boolean isApprover(String username)
    {
        return username != null && configuredApprovers.contains(username.trim());
    }

    /** 完整审批资格判定：admin / 权限字符 / 配置名单；供 Controller 方法体纵深校验 */
    public boolean canApprove(LoginUser loginUser)
    {
        if (loginUser == null || loginUser.getUser() == null)
        {
            return false;
        }
        if (SecurityUtils.isAdmin(loginUser.getUserId()))
        {
            return true;
        }
        Set<String> permissions = loginUser.getPermissions();
        if (permissions != null && permissions.contains(APPROVE_PERM))
        {
            return true;
        }
        return isApprover(loginUser.getUsername());
    }

    /** 配置名单快照（只读，便于诊断与单测断言） */
    public Set<String> getConfiguredApprovers()
    {
        return configuredApprovers;
    }
}
