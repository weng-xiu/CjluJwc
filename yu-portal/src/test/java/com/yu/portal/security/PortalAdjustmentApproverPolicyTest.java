package com.yu.portal.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 调停课审批人策略单测（S-5 门户审批角色化）。
 *
 * <p>覆盖三条放行路径与两条拒绝路径：admin 兜底、权限字符 {@code portal:adjustment:approve}、
 * 配置名单（单值/多值），以及无任何资格时的拒绝。策略是纯函数（构造时读配置），
 * 因此不需要 Spring 上下文。
 */
class PortalAdjustmentApproverPolicyTest
{
    private static LoginUser loginUser(Long userId, String username, Set<String> permissions)
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(username);
        return new LoginUser(userId, 2L, user, permissions);
    }

    @Test
    @DisplayName("isApprover：单值配置与逗号名单均命中，空值与空名不命中")
    void isApprover_matchesConfiguredNames()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("admin", " jwc01 , jwc02 ");
        assertTrue(policy.isApprover("admin"));
        assertTrue(policy.isApprover("jwc01"));
        assertTrue(policy.isApprover("jwc02"));
        assertTrue(policy.isApprover(" jwc02 "));
        assertFalse(policy.isApprover("teacher01"));
        assertFalse(policy.isApprover(null));
        assertFalse(policy.isApprover(""));
        assertFalse(new PortalAdjustmentApproverPolicy(null, null).isApprover("anyone"));
    }

    @Test
    @DisplayName("canApprove：超级管理员放行")
    void canApprove_adminAlwaysAllowed()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("nobody", "");
        assertTrue(policy.canApprove(loginUser(1L, "tester", Collections.emptySet())));
    }

    @Test
    @DisplayName("canApprove：持有 portal:adjustment:approve 权限字符即放行（角色分配生效，无需重启）")
    void canApprove_permissionCharacterAllowed()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("nobody", "");
        assertTrue(policy.canApprove(
                loginUser(200L, "teacher01", Set.of(PortalAdjustmentApproverPolicy.APPROVE_PERM))));
    }

    @Test
    @DisplayName("canApprove：配置名单内用户名放行")
    void canApprove_configuredUsernameAllowed()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("admin", "jwc01");
        assertTrue(policy.canApprove(loginUser(300L, "jwc01", Collections.emptySet())));
    }

    @Test
    @DisplayName("canApprove：普通教师无权限字符且不在名单时拒绝")
    void canApprove_plainTeacherRejected()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("admin", "jwc01");
        assertFalse(policy.canApprove(loginUser(400L, "teacher02", Collections.emptySet())));
        assertFalse(policy.canApprove(loginUser(400L, "teacher02", Set.of("portal:adjustment:list"))));
    }

    @Test
    @DisplayName("canApprove：未登录/空用户对象一律拒绝")
    void canApprove_nullRejected()
    {
        PortalAdjustmentApproverPolicy policy = new PortalAdjustmentApproverPolicy("admin", "jwc01");
        assertFalse(policy.canApprove(null));
        assertFalse(policy.canApprove(new LoginUser()));
    }
}
