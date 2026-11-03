package com.yu.framework.manager;

import java.util.TimerTask;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.yu.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器
 * 
 * @author ruoyi
 */
public class AsyncManager
{
    /**
     * 异步操作任务线程池（T-6）：审计日志专用池，有界队列 + 队满回压，详见 {@code ThreadPoolConfig}
     */
    private ThreadPoolTaskExecutor executor = SpringUtils.getBean("asyncLogExecutor");

    /**
     * 单例模式
     */
    private AsyncManager(){}

    private static AsyncManager me = new AsyncManager();

    public static AsyncManager me()
    {
        return me;
    }

    /**
     * 执行任务
     * 
     * @param task 任务
     */
    public void execute(TimerTask task)
    {
        // 原实现用 schedule(task, 10ms) 把日志推迟出请求窗口；换成专用池后不再需要“先拖 10ms”这种
        // 粗糙手段——日志本来就在别的线程上跑，再拖只会拉长停机时的排空时间
        executor.execute(task);
    }

    /**
     * 停止任务线程池
     */
    public void shutdown()
    {
        // 池的 waitForTasksToCompleteOnShutdown/awaitTermination 已在 ThreadPoolConfig 中设定
        executor.shutdown();
    }
}
