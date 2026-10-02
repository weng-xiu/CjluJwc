package com.yu.framework.config;

import com.yu.common.utils.MdcUtils;
import com.yu.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author ruoyi
 **/
@Configuration
public class ThreadPoolConfig
{
    // 核心线程池大小
    private int corePoolSize = 50;

    // 最大可创建的线程数
    private int maxPoolSize = 200;

    // 队列最大长度
    private int queueCapacity = 1000;

    // 线程池维护线程所允许的空闲时间
    private int keepAliveSeconds = 300;

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // V4.0 A5：异步任务携带提交线程的 traceId，否则选课/报盘这类异步链路在日志里断开
        executor.setTaskDecorator(new MdcTaskDecorator());
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * MDC 上下文传递装饰器：提交时快照 traceId，执行时恢复，结束后清理（线程复用会串号）
     */
    static class MdcTaskDecorator implements TaskDecorator
    {
        @Override
        public Runnable decorate(Runnable runnable)
        {
            Map<String, String> context = MdcUtils.copyContext();
            return () -> {
                Map<String, String> previous = MDC.getCopyOfContextMap();
                context.forEach(MDC::put);
                try
                {
                    runnable.run();
                }
                finally
                {
                    MDC.clear();
                    if (previous != null)
                    {
                        previous.forEach(MDC::put);
                    }
                }
            };
        }
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(corePoolSize,
            BasicThreadFactory.builder().namingPattern("schedule-pool-%d").daemon(true).build(),
            new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }
}
