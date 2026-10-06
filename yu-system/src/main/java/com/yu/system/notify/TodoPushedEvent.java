package com.yu.system.notify;

import com.yu.system.domain.SysTodo;

/**
 * 待办推送事件（A2 事件驱动最小版）。
 *
 * <p>业务服务不再同步直调 {@code ISysTodoService.createTodo}，而是发布本事件，由
 * {@link NotificationEventHandler} 在业务事务提交后异步落库。好处：
 * <ul>
 *   <li>主业务与消息/待办通道解耦，通道异常不阻断、不回滚主业务；</li>
 *   <li>"成绩提交→GPA→预警→推送"等链路可通过事件编排扩展。</li>
 * </ul>
 *
 * @author A2
 */
public class TodoPushedEvent
{
    private final SysTodo todo;

    public TodoPushedEvent(SysTodo todo)
    {
        this.todo = todo;
    }

    public SysTodo getTodo()
    {
        return todo;
    }
}
