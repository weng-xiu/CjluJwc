package com.yu.system.servicedesk;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 服务事项目录注册表（N3 一站式服务大厅）。
 *
 * <p>集中登记全校可办理的服务事项及其时效基线，供时效看板分档与超时催办判定使用。
 * 事项编码与统一待办的 {@code businessType} 对齐（见 A2 各业务服务推送待办时的取值）。
 * 未知编码回落到 {@link #defaultItem()}，保证任何来源的待办都能纳入看板统计。
 *
 * @author N3
 */
@Component
public class ServiceItemCatalog
{
    /** 未匹配事项时的兜底时效（小时） */
    private static final int DEFAULT_SLA_HOURS = 48;

    private final Map<String, ServiceItemDef> items = new LinkedHashMap<>();

    public ServiceItemCatalog()
    {
        register(new ServiceItemDef("statusChange", "学籍异动审批", "审批", 48));
        register(new ServiceItemDef("thesis", "学位论文审核", "审批", 72));
        register(new ServiceItemDef("gradeReview", "成绩复核", "审批", 24));
        register(new ServiceItemDef("scheduleAdjust", "调停课审批", "审批", 24));
        register(new ServiceItemDef("classroomBorrow", "教室借用审批", "审批", 24));
        register(new ServiceItemDef("warningAssist", "学业预警帮扶", "服务", 120));
        register(new ServiceItemDef("graduationProcedure", "毕业手续办理", "服务", 168));
        register(new ServiceItemDef("certReissue", "证书补办申请", "服务", 168));
        register(new ServiceItemDef("dis_sync", "数据同步异常处置", "运维", 8));
        register(new ServiceItemDef("countersign", "OA 加签办理", "审批", 24));
    }

    /** 登记（或覆盖）一个服务事项 */
    public void register(ServiceItemDef def)
    {
        items.put(def.code(), def);
    }

    /** 按编码解析事项定义，缺省回落 */
    public ServiceItemDef resolve(String code)
    {
        ServiceItemDef def = code == null ? null : items.get(code);
        return def != null ? def : defaultItem();
    }

    /** 全部目录项，用于服务大厅前端渲染事项卡片 */
    public Collection<ServiceItemDef> list()
    {
        return items.values();
    }

    /** 兜底事项：编码 OTHER，SLA {@value DEFAULT_SLA_HOURS} 小时 */
    public ServiceItemDef defaultItem()
    {
        return new ServiceItemDef("OTHER", "其他服务事项", "服务", DEFAULT_SLA_HOURS);
    }
}
