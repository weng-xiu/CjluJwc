package com.yu.system.servicedesk;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.notify.MessagePushedEvent;
import com.yu.system.service.ISysTodoService;

/**
 * 一站式服务大厅实现（N3）。
 *
 * <p>时效看板与超时催办均为对统一待办 {@code sys_todo} 的只读聚合，无需新增表；
 * 催办提醒复用 A2 的事件通道下发，天然具备故障隔离能力。
 *
 * @author N3
 */
@Service
public class ServiceDeskServiceImpl implements IServiceDeskService
{
    /** 待办状态：0 待办 */
    private static final String STATUS_PENDING = "0";

    /** 消息类型：1 通知 */
    private static final String MSG_TYPE_NOTICE = "1";

    /** 一小时毫秒数 */
    private static final long HOUR_MILLIS = 3600_000L;

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ServiceItemCatalog catalog;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public Collection<ServiceItemDef> catalog()
    {
        return catalog.list();
    }

    @Override
    public ServiceDashboardVo dashboard()
    {
        return computeDashboard(loadPending(), System.currentTimeMillis());
    }

    @Override
    public int urgeOverdue()
    {
        long now = System.currentTimeMillis();
        int urged = 0;
        for (SysTodo todo : loadPending())
        {
            ServiceItemDef def = catalog.resolve(todo.getBusinessType());
            long elapsedHours = elapsedHours(todo, now);
            if (def.levelOf(elapsedHours) == ServiceItemDef.AgingLevel.OVERDUE)
            {
                eventPublisher.publishEvent(new MessagePushedEvent(buildUrgeMessage(todo, def, elapsedHours)));
                urged++;
            }
        }
        return urged;
    }

    /** 加载全校待办（status=0） */
    private List<SysTodo> loadPending()
    {
        SysTodo query = new SysTodo();
        query.setStatus(STATUS_PENDING);
        return sysTodoService.selectTodoList(query);
    }

    /**
     * 计算时效看板（包内可见，便于以固定 now 做单元测试）。
     *
     * @param pending    待办列表
     * @param nowMillis  当前时间基准（毫秒）
     */
    ServiceDashboardVo computeDashboard(List<SysTodo> pending, long nowMillis)
    {
        ServiceDashboardVo vo = new ServiceDashboardVo();
        vo.setGeneratedAt(new Date(nowMillis));
        if (pending == null || pending.isEmpty())
        {
            return vo;
        }

        Map<String, ServiceDashboardVo.ItemStat> statMap = new LinkedHashMap<>();
        int normal = 0;
        int nearDue = 0;
        int overdue = 0;

        for (SysTodo todo : pending)
        {
            ServiceItemDef def = catalog.resolve(todo.getBusinessType());
            long elapsedHours = elapsedHours(todo, nowMillis);
            ServiceItemDef.AgingLevel level = def.levelOf(elapsedHours);

            ServiceDashboardVo.ItemStat stat = statMap.get(def.code());
            if (stat == null)
            {
                stat = new ServiceDashboardVo.ItemStat();
                stat.setCode(def.code());
                stat.setName(def.name());
                stat.setCategory(def.category());
                stat.setSlaHours(def.slaHours());
                statMap.put(def.code(), stat);
            }
            stat.setTotal(stat.getTotal() + 1);
            stat.setOldestHours(Math.max(stat.getOldestHours(), elapsedHours));
            switch (level)
            {
                case OVERDUE:
                    stat.setOverdue(stat.getOverdue() + 1);
                    overdue++;
                    break;
                case NEAR_DUE:
                    stat.setNearDue(stat.getNearDue() + 1);
                    nearDue++;
                    break;
                default:
                    stat.setNormal(stat.getNormal() + 1);
                    normal++;
                    break;
            }
        }

        vo.setTotalPending(pending.size());
        vo.setNormalCount(normal);
        vo.setNearDueCount(nearDue);
        vo.setOverdueCount(overdue);
        vo.setItems(new java.util.ArrayList<>(statMap.values()));
        return vo;
    }

    /** 计算待办已挂起小时数，无创建时间的按 0 处理 */
    private long elapsedHours(SysTodo todo, long nowMillis)
    {
        Date createTime = todo.getCreateTime();
        if (createTime == null)
        {
            return 0L;
        }
        long delta = nowMillis - createTime.getTime();
        return delta <= 0 ? 0L : delta / HOUR_MILLIS;
    }

    /** 构造催办提醒消息 */
    private SysMessage buildUrgeMessage(SysTodo todo, ServiceItemDef def, long elapsedHours)
    {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(todo.getReceiverId());
        msg.setMsgType(MSG_TYPE_NOTICE);
        msg.setTitle("【超时催办】" + def.name() + "：" + todo.getTitle());
        msg.setContent("该事项已挂起 " + elapsedHours + " 小时，超过时效基线 " + def.slaHours() + " 小时，请尽快办理。");
        msg.setBusinessType("serviceDeskUrge");
        msg.setBusinessId(todo.getTodoId());
        msg.setCreateBy("system");
        return msg;
    }
}
