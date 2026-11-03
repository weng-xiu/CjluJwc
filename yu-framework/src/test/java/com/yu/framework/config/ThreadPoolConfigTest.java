package com.yu.framework.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.yu.common.utils.MdcUtils;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 线程池拆池（T-6）的语义测试。
 *
 * <p>钉住四件事：① 队列真的有限（不能靠 1000 深队列掩盖积压）；
 * ② 长任务池队满即拒绝，不回落请求线程（否则"异步"会悄悄变"同步超时"）；
 * ③ 审计日志池队满时回压到提交线程，绝不丢弃；
 * ④ 拒绝次数被计数（这是运维判断"池到底够不够"的唯一依据），且 traceId 能跨线程传递。
 */
class ThreadPoolConfigTest
{
    private final List<ThreadPoolTaskExecutor> started = new ArrayList<>();

    @AfterEach
    void stopPools()
    {
        started.forEach(ThreadPoolTaskExecutor::shutdown);
        MDC.clear();
    }

    @Test
    @DisplayName("长任务池：并发+队列都占满后拒绝提交（不再有无界积压）")
    void lotteryPool_rejectsWhenSaturated() throws Exception
    {
        ThreadPoolConfig config = newConfig();
        ThreadPoolTaskExecutor executor = start(config.lotteryTaskExecutor());
        CountDownLatch release = new CountDownLatch(1);
        // 1 核心 + 8 队列 + 1 扩容 = 10 个任务可被接纳
        int accepted = 10;
        for (int i = 0; i < accepted; i++)
        {
            executor.execute(() -> awaitQuietly(release));
        }

        RejectedExecutionException ex = assertThrows(RejectedExecutionException.class,
                () -> executor.execute(() -> { }));
        release.countDown();
        // Spring 的 ThreadPoolTaskExecutor 会把拒绝包一层 TaskRejectedException，池名在 cause 里
        String reason = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        assertTrue(reason != null && reason.contains("lottery"), "异常需指明是哪个池拒绝的，实际=" + reason);
        assertEquals(1L, config.rejectedCount("lottery"), "拒绝次数必须被计数（T-6 告警口径）");
    }

    @Test
    @DisplayName("审计日志池：队满时回压到提交线程执行，不丢日志")
    void asyncLogPool_backpressuresInsteadOfDropping() throws Exception
    {
        ThreadPoolConfig config = newConfig();
        ThreadPoolTaskExecutor executor = start(config.asyncLogExecutor());
        CountDownLatch release = new CountDownLatch(1);
        // 4 核心 + 512 队列 + 4 扩容 = 520 个任务可被接纳，第 521 个触发回压
        for (int i = 0; i < 520; i++)
        {
            executor.execute(() -> awaitQuietly(release));
        }
        AtomicBoolean ranOnSubmittingThread = new AtomicBoolean(false);

        executor.execute(() -> ranOnSubmittingThread.set(true));
        release.countDown();

        assertTrue(ranOnSubmittingThread.get(), "队满时审计日志必须回压到提交线程执行，而不是被丢弃");
        assertEquals(1L, config.rejectedCount("async-log"), "回压同样要计数：它是池容量不足的信号");
    }

    @Test
    @DisplayName("traceId 跨线程传递：异步任务能看到提交线程的 traceId，执行完不残留")
    void mdcTraceIdPropagatesToWorkerThread() throws Exception
    {
        ThreadPoolConfig config = newConfig();
        ThreadPoolTaskExecutor executor = start(config.exportTaskExecutor());
        MdcUtils.put("trace-abc-123");
        AtomicReference<String> seen = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        executor.execute(() -> {
            seen.set(MDC.get(MdcUtils.TRACE_ID));
            done.countDown();
        });

        assertTrue(done.await(5, TimeUnit.SECONDS), "任务应在 5 秒内完成");
        assertEquals("trace-abc-123", seen.get(), "异步日志必须带得上 traceId，否则链路在池边界断开");
        // 线程复用不能串号：提交线程先清掉自己的 traceId，worker 线程也不应把上个任务的值带过来
        AtomicReference<String> leaked = new AtomicReference<>("unset");
        CountDownLatch done2 = new CountDownLatch(1);
        MdcUtils.clear();
        executor.execute(() -> {
            leaked.set(String.valueOf(MDC.get(MdcUtils.TRACE_ID)));
            done2.countDown();
        });
        assertTrue(done2.await(5, TimeUnit.SECONDS));
        assertEquals("null", leaked.get(), "上一个任务的 traceId 不能残留到下一个任务");
    }

    @Test
    @DisplayName("任务异常逃逸到池边界时被记录，不产生无主线程（失败不拖垮池）")
    void taskFailureIsContained() throws Exception
    {
        ThreadPoolConfig config = newConfig();
        ThreadPoolTaskExecutor executor = start(config.exportTaskExecutor());
        CountDownLatch done = new CountDownLatch(1);

        executor.execute(() -> {
            done.countDown();
            throw new IllegalStateException("boom");
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        // 池仍可用：一次任务异常不应影响后续提交
        CountDownLatch after = new CountDownLatch(1);
        executor.execute(after::countDown);
        assertTrue(after.await(5, TimeUnit.SECONDS), "单个任务抛异常不应让池失去可用性");
    }

    /** 走真实配置方法，但不经过 Spring 容器；MeterRegistry 缺省（单测环境），指标绑定应静默跳过。 */
    private ThreadPoolConfig newConfig()
    {
        ObjectProvider<MeterRegistry> noRegistry = new DefaultListableBeanFactory()
                .getBeanProvider(MeterRegistry.class);
        return new ThreadPoolConfig(noRegistry);
    }

    private ThreadPoolTaskExecutor start(ThreadPoolTaskExecutor executor)
    {
        executor.initialize();
        started.add(executor);
        return executor;
    }

    private static void awaitQuietly(CountDownLatch latch)
    {
        try
        {
            latch.await(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
