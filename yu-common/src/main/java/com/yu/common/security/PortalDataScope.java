package com.yu.common.security;

import java.util.function.BiConsumer;
import com.yu.common.constant.HttpStatus;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;

/**
 * 门户/自助端数据范围集中策略（A4 数据权限体系化）。
 *
 * <p>此前门户各 Controller 分散地手写 {@code query.setXxxId(getUserId())} 来把查询收敛到"本人"，
 * 口径不统一、易遗漏（漏一次即越权），也无法在单一位置审计隔离是否有效。本工具把该逻辑收敛为：
 * <ul>
 *   <li>{@link #restrictToSelf} —— 强制把查询对象的归属字段写为当前登录用户，屏蔽客户端传入的越权值；</li>
 *   <li>{@link #assertSelf} —— 校验目标用户 ID 是否属于当前登录用户（管理员放行），用于按 ID 操作的写接口。</li>
 * </ul>
 *
 * <p>所有门户"仅本人"口径统一经此出口，便于审计与后续接入统一数据权限中心。
 *
 * @author A4
 */
public final class PortalDataScope
{
    private PortalDataScope()
    {
    }

    /**
     * 将查询对象的归属字段强制限定为当前登录用户，覆盖客户端可能伪造的值。
     *
     * @param query       查询对象
     * @param ownerSetter 归属字段写入器，例如 {@code (q, id) -> q.setStudentId(id)}
     * @param <Q>         查询对象类型
     * @return 同一个 query，便于链式使用
     */
    public static <Q> Q restrictToSelf(Q query, BiConsumer<Q, Long> ownerSetter)
    {
        if (query == null || ownerSetter == null)
        {
            return query;
        }
        ownerSetter.accept(query, SecurityUtils.getUserId());
        return query;
    }

    /**
     * 校验目标用户 ID 属于当前登录用户；不一致且非管理员时抛出越权异常。
     *
     * @param targetUserId 待访问数据归属的用户 ID，可为 null（表示不做归属约束）
     */
    public static void assertSelf(Long targetUserId)
    {
        Long currentUserId = SecurityUtils.getUserId();
        if (targetUserId != null && !targetUserId.equals(currentUserId) && !SecurityUtils.isAdmin(currentUserId))
        {
            throw new ServiceException("无权访问他人数据", HttpStatus.FORBIDDEN);
        }
    }
}
