package com.yu.system.notify;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;

/**
 * A2 事件驱动最小版：{@link NotificationEventHandler} 单元验证。
 *
 * <p>验收标准：
 * <ul>
 *   <li>事件到达后由处理器委托落库（createTodo/sendMessage）；</li>
 *   <li>通知通道异常被吞掉，绝不向上抛出（保证主业务不被消息通道故障阻断）。</li>
 * </ul>
 *
 * @author A2
 */
@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest
{
    @Mock
    private ISysTodoService sysTodoService;

    @Mock
    private ISysMessageService sysMessageService;

    @InjectMocks
    private NotificationEventHandler handler;

    @Test
    @DisplayName("待办事件-委托落库")
    void onTodoPushed_delegatesToCreateTodo()
    {
        SysTodo todo = new SysTodo();
        when(sysTodoService.createTodo(any(SysTodo.class))).thenReturn(1);

        handler.onTodoPushed(new TodoPushedEvent(todo));

        verify(sysTodoService).createTodo(todo);
    }

    @Test
    @DisplayName("消息事件-委托落库")
    void onMessagePushed_delegatesToSendMessage()
    {
        SysMessage msg = new SysMessage();
        when(sysMessageService.sendMessage(any(SysMessage.class))).thenReturn(1);

        handler.onMessagePushed(new MessagePushedEvent(msg));

        verify(sysMessageService).sendMessage(msg);
    }

    @Test
    @DisplayName("待办落库抛异常-被隔离，不阻断主业务")
    void onTodoPushed_swallowsException()
    {
        doThrow(new RuntimeException("消息通道宕机")).when(sysTodoService).createTodo(any());

        assertDoesNotThrow(() -> handler.onTodoPushed(new TodoPushedEvent(new SysTodo())));
    }

    @Test
    @DisplayName("消息落库抛异常-被隔离，不阻断主业务")
    void onMessagePushed_swallowsException()
    {
        doThrow(new RuntimeException("消息通道宕机")).when(sysMessageService).sendMessage(any());

        assertDoesNotThrow(() -> handler.onMessagePushed(new MessagePushedEvent(new SysMessage())));
    }

    @Test
    @DisplayName("空事件-安全跳过，不触碰服务")
    void nullEvent_isIgnored()
    {
        handler.onTodoPushed(null);
        handler.onMessagePushed(null);
        handler.onTodoPushed(new TodoPushedEvent(null));

        verifyNoInteractions(sysTodoService, sysMessageService);
    }
}
