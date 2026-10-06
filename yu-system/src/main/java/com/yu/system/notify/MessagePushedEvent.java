package com.yu.system.notify;

import com.yu.system.domain.SysMessage;

/**
 * 消息推送事件（A2 事件驱动最小版）。
 *
 * <p>业务服务发布本事件替代同步直调 {@code ISysMessageService.sendMessage}，
 * 由 {@link NotificationEventHandler} 在业务事务提交后处理，实现主业务与通知通道解耦。
 *
 * @author A2
 */
public class MessagePushedEvent
{
    private final SysMessage message;

    public MessagePushedEvent(SysMessage message)
    {
        this.message = message;
    }

    public SysMessage getMessage()
    {
        return message;
    }
}
