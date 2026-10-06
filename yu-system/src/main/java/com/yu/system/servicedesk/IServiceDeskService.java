package com.yu.system.servicedesk;

import java.util.Collection;

/**
 * 一站式服务大厅服务（N3）。
 *
 * <p>在既有统一待办/消息/OA 任务之上补齐三项标杆能力：
 * <ul>
 *   <li>事项目录：{@link #catalog()} 提供全校可办理事项清单；</li>
 *   <li>时效看板：{@link #dashboard()} 汇总待办按事项与时效档位的分布；</li>
 *   <li>超时催办：{@link #urgeOverdue()} 扫描超时待办并经事件通道下发催办提醒。</li>
 * </ul>
 *
 * @author N3
 */
public interface IServiceDeskService
{
    /** 事项目录 */
    Collection<ServiceItemDef> catalog();

    /** 时效看板：按当前待办实时计算 */
    ServiceDashboardVo dashboard();

    /**
     * 超时催办：扫描超过时效基线的待办，向接收人下发催办提醒（复用 A2 事件通道）。
     *
     * @return 触发催办的待办数量
     */
    int urgeOverdue();
}
