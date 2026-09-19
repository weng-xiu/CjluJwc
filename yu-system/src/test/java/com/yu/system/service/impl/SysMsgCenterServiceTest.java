package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.mapper.SysMessageMapper;
import com.yu.system.mapper.SysTodoMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * P1 统一消息与待办中心 单元测试
 * 验收标准：审批/预警自动产生待办
 */
@ExtendWith(MockitoExtension.class)
class SysMsgCenterServiceTest
{
    @Mock
    private SysMessageMapper sysMessageMapper;

    @Mock
    private SysTodoMapper sysTodoMapper;

    @InjectMocks
    private SysMessageServiceImpl messageService;

    @InjectMocks
    private SysTodoServiceImpl todoService;

    @Test
    @DisplayName("发送审批消息-自动设置readStatus=0")
    void sendMessage_setsReadStatus()
    {
        SysMessage msg = new SysMessage();
        msg.setReceiverId(1L);
        msg.setMsgType("1"); // 审批
        msg.setTitle("学籍异动待审批");
        when(sysMessageMapper.insert(any())).thenReturn(1);

        int rows = messageService.sendMessage(msg);
        assertEquals(1, rows);
        assertEquals("0", msg.getReadStatus());
        assertNotNull(msg.getCreateTime());
    }

    @Test
    @DisplayName("批量发送消息")
    void batchSendMessages()
    {
        List<SysMessage> list = new ArrayList<>();
        SysMessage m1 = new SysMessage();
        m1.setReceiverId(1L); m1.setMsgType("0"); m1.setTitle("预警1");
        SysMessage m2 = new SysMessage();
        m2.setReceiverId(2L); m2.setMsgType("0"); m2.setTitle("预警2");
        list.add(m1); list.add(m2);
        when(sysMessageMapper.batchInsert(any())).thenReturn(2);

        int rows = messageService.batchSendMessages(list);
        assertEquals(2, rows);
        assertEquals("0", m1.getReadStatus());
        assertEquals("0", m2.getReadStatus());
    }

    @Test
    @DisplayName("创建待办-自动设置status=0")
    void createTodo_setsStatus()
    {
        SysTodo todo = new SysTodo();
        todo.setReceiverId(1L);
        todo.setTodoType("1");
        todo.setTitle("审批待办");
        when(sysTodoMapper.insert(any())).thenReturn(1);

        int rows = todoService.createTodo(todo);
        assertEquals(1, rows);
        assertEquals("0", todo.getStatus());
        assertNotNull(todo.getCreateTime());
    }

    @Test
    @DisplayName("完成待办")
    void completeTodo()
    {
        when(sysTodoMapper.complete(1L, 100L)).thenReturn(1);
        int rows = todoService.completeTodo(1L, 100L);
        assertEquals(1, rows);
        verify(sysTodoMapper).complete(1L, 100L);
    }

    @Test
    @DisplayName("未读消息计数")
    void countUnread()
    {
        when(sysMessageMapper.countUnread(1L)).thenReturn(5);
        assertEquals(5, messageService.countUnread(1L));
    }

    @Test
    @DisplayName("待办计数")
    void countPending()
    {
        when(sysTodoMapper.countPending(1L)).thenReturn(3);
        assertEquals(3, todoService.countPending(1L));
    }
}
