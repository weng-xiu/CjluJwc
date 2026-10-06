package com.yu.system.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;

/**
 * 通知事件处理器（A2 事件驱动最小版）。
 *
 * <p>以 {@link TransactionalEventListener} 在业务事务 {@code AFTER_COMMIT} 阶段消费
 * {@link TodoPushedEvent}/{@link MessagePushedEvent}：
 * <ul>
 *   <li>业务未提交（回滚）时不会推送，避免脏通知；</li>
 *   <li>{@code fallbackExecution = true} 保证无事务上下文（如定时任务）时仍即时执行；</li>
 *   <li>处理体内吞掉异常并记录日志 —— 消息/待办通道故障只影响通知，绝不阻断或回滚已提交的主业务。</li>
 * </ul>
 *
 * @author A2
 */
@Component
public class NotificationEventHandler
{
    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ISysMessageService sysMessageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTodoPushed(TodoPushedEvent event)
    {
        if (event == null || event.getTodo() == null)
        {
            return;
        }
        try
        {
            sysTodoService.createTodo(event.getTodo());
        }
        catch (Exception e)
        {
            // 故障隔离：通知通道异常不影响已提交的主业务
            log.error("待办事件处理失败，已隔离不影响主业务: businessId={}, type={}",
                    event.getTodo().getBusinessId(), event.getTodo().getTodoType(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMessagePushed(MessagePushedEvent event)
    {
        if (event == null || event.getMessage() == null)
        {
            return;
        }
        try
        {
            sysMessageService.sendMessage(event.getMessage());
        }
        catch (Exception e)
        {
            log.error("消息事件处理失败，已隔离不影响主业务: businessId={}, type={}",
                    event.getMessage().getBusinessId(), event.getMessage().getMsgType(), e);
        }
    }
}
