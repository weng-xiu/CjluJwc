package com.yu.system.servicedesk;

/**
 * 服务大厅事项目录定义（N3 一站式服务大厅）。
 *
 * <p>描述一个可办理的"服务事项"：编码、名称、分类与时效基线（SLA 小时数）。
 * 时效看板据此对待办按"正常/临期/超时"分档，超时催办据此判定是否触发提醒。
 *
 * @param code     事项编码（与待办 businessType 对齐）
 * @param name     事项展示名称
 * @param category 事项分类（审批/服务/运维…）
 * @param slaHours 时效基线（小时），待办挂起超过该时长判定为超时
 * @author N3
 */
public record ServiceItemDef(String code, String name, String category, int slaHours)
{
    /** 临期阈值比例：已耗时达到 SLA 的该比例即视为"临期" */
    public static final double NEAR_DUE_RATIO = 0.7d;

    /** 给定已耗时小时数，判断所处时效档位 */
    public AgingLevel levelOf(long elapsedHours)
    {
        if (elapsedHours > slaHours)
        {
            return AgingLevel.OVERDUE;
        }
        if (elapsedHours >= (long) Math.ceil(slaHours * NEAR_DUE_RATIO))
        {
            return AgingLevel.NEAR_DUE;
        }
        return AgingLevel.NORMAL;
    }

    /** 时效档位 */
    public enum AgingLevel
    {
        /** 正常（未达临期线） */
        NORMAL,
        /** 临期（接近时效基线） */
        NEAR_DUE,
        /** 超时（超过时效基线） */
        OVERDUE
    }
}
