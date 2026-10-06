package com.yu.system.servicedesk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.yu.system.domain.SysTodo;
import com.yu.system.notify.MessagePushedEvent;
import com.yu.system.service.ISysTodoService;

/**
 * N3 一站式服务大厅：时效看板分档与超时催办的单元验证。
 *
 * @author N3
 */
@ExtendWith(MockitoExtension.class)
class ServiceDeskServiceTest
{
    private static final long HOUR = 3600_000L;

    @Mock
    private ISysTodoService sysTodoService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ServiceItemCatalog catalog = new ServiceItemCatalog();

    @InjectMocks
    private ServiceDeskServiceImpl service;

    private SysTodo todo(String businessType, long createdAtMillis)
    {
        SysTodo t = new SysTodo();
        t.setTodoId(1L);
        t.setReceiverId(100L);
        t.setTitle(businessType + " 事项");
        t.setBusinessType(businessType);
        t.setCreateTime(new Date(createdAtMillis));
        return t;
    }

    @Test
    @DisplayName("时效看板-正常/临期/超时正确分档")
    void dashboard_bucketsAging()
    {
        long now = 1_700_000_000_000L;
        List<SysTodo> pending = new ArrayList<>();
        pending.add(todo("gradeReview", now - 30 * HOUR));  // SLA 24h -> OVERDUE
        pending.add(todo("gradeReview", now - 18 * HOUR));  // SLA 24h -> NEAR_DUE
        pending.add(todo("warningAssist", now - 10 * HOUR)); // SLA 120h -> NORMAL

        ServiceDashboardVo vo = service.computeDashboard(pending, now);

        assertEquals(3, vo.getTotalPending());
        assertEquals(1, vo.getOverdueCount());
        assertEquals(1, vo.getNearDueCount());
        assertEquals(1, vo.getNormalCount());

        ServiceDashboardVo.ItemStat gradeReview = vo.getItems().stream()
                .filter(i -> "gradeReview".equals(i.getCode())).findFirst().orElseThrow();
        assertEquals(2, gradeReview.getTotal());
        assertEquals(1, gradeReview.getOverdue());
        assertEquals(1, gradeReview.getNearDue());
    }

    @Test
    @DisplayName("未知 businessType 回落 OTHER 仍纳入统计")
    void dashboard_unknownFallsBack()
    {
        long now = 1_700_000_000_000L;
        List<SysTodo> pending = List.of(todo("someUnknownFlow", now - 100 * HOUR));

        ServiceDashboardVo vo = service.computeDashboard(pending, now);

        assertEquals(1, vo.getOverdueCount());
        assertEquals("OTHER", vo.getItems().get(0).getCode());
    }

    @Test
    @DisplayName("超时催办-仅对超期待办经事件通道下发提醒")
    void urgeOverdue_publishesForOverdueOnly()
    {
        when(sysTodoService.selectTodoList(any(SysTodo.class))).thenReturn(new ArrayList<>(List.of(
                todo("gradeReview", System.currentTimeMillis() - 30 * HOUR),  // 超时
                todo("gradeReview", System.currentTimeMillis() - 1 * HOUR)))); // 正常

        int urged = service.urgeOverdue();

        assertEquals(1, urged);
        verify(eventPublisher, times(1)).publishEvent(any(MessagePushedEvent.class));
    }

    @Test
    @DisplayName("空待办-看板与催办均为零，不触碰事件通道")
    void emptyPending_isSafe()
    {
        when(sysTodoService.selectTodoList(any(SysTodo.class))).thenReturn(new ArrayList<>());

        ServiceDashboardVo vo = service.dashboard();
        int urged = service.urgeOverdue();

        assertEquals(0, vo.getTotalPending());
        assertEquals(0, urged);
        assertEquals(0, mockingDetails(eventPublisher).getInvocations().size());
    }
}
