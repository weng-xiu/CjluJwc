package com.yu.framework.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.yu.common.utils.MdcUtils;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 线程池配置（T-6：按用途拆池 + 有界队列 + 拒绝计数）。
 *
 * <p>原先只有一个 {@code threadPoolTaskExecutor}（50/200/1000 + {@code CallerRunsPolicy}），
 * 它同时承担着三种互相冲突的语义，实际是三类故障的温床：
 * <ul>
 *   <li>队列深达 1000，队满几乎不可能被发现，积压只能靠用户投诉暴露；</li>
 *   <li>队满后 {@code CallerRunsPolicy} 把任务回落到提交线程（HTTP 线程）同步执行，
 *       对抽签/导出/AI 这类长任务等于"异步悄悄降级成同步"，表现为接口超时且无日志；</li>
 *   <li>所有用途共用一个池，一次大批量导出能把审计日志写入一起拖死。</li>
 * </ul>
 *
 * <p>现在按用途各一个池，队列全部有界，拒绝一律计数并限频告警（暴露为
 * {@code yu_executor_rejected_total{pool=...}}）。拒绝策略按"丢了会怎样"分两类：
 * <ul>
 *   <li>{@link Overflow#ABORT}（长任务：抽签/导出/AI）—— 拒绝即抛，由调用方明确失败并提示重试。
 *       这些任务绝不能回落到请求线程，否则用户看到的是超时而不是"排队满了"。</li>
 *   <li>{@link Overflow#BACKPRESSURE}（审计日志）—— 队满时回到提交线程执行。审计日志丢一条就是
 *       合规缺口，宁可让请求慢一点也不能丢；这类任务由请求方产生、天然可以回压。</li>
 * </ul>
 *
 * <p>线程数/队列长度在这里硬编码而不外置配置：这些数值是"能扛多少并发、丢不丢得起"的设计决定，
 * 不是环境差异，放配置里只会让不同环境跑出不同故障形态。
 *
 * @author ruoyi
 **/
@Configuration
public class ThreadPoolConfig
{
    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    /** 线程空闲回收时间（秒） */
    private static final int KEEP_ALIVE_SECONDS = 300;

    /** 停机时等待在途任务的时间（秒）：审计日志必须落完，长任务给一个明确的收尾窗口 */
    private static final int AWAIT_TERMINATION_SECONDS = 30;

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    /** 各池累计拒绝次数：既作为指标来源，也便于运维/测试直接读取 */
    private final Map<String, AtomicLong> rejections = new ConcurrentHashMap<>();

    public ThreadPoolConfig(ObjectProvider<MeterRegistry> meterRegistryProvider)
    {
        this.meterRegistryProvider = meterRegistryProvider;
    }

    /**
     * 长任务池（抽签等"单轮次全量重算"）。
     *
     * <p>并发度刻意压低（1~2 线程）：抽签本身是轮次级串行全量重算，多开线程只会争抢 DB 行锁。
     */
    @Bean(name = "lotteryTaskExecutor")
    public ThreadPoolTaskExecutor lotteryTaskExecutor()
    {
        return boundedExecutor("lottery", 1, 2, 8, Overflow.ABORT);
    }

    /**
     * 导出池：Excel/报表生成并落盘。
     *
     * <p>当前导出仍在请求线程内同步执行，此池是异步化的接入点（创建 bean 不会预先起线程）。
     * 队列取 32：单个导出任务秒级，32 个排队已足够吸收突发，再多说明该做任务表而不是加队列。
     */
    @Bean(name = "exportTaskExecutor")
    public ThreadPoolTaskExecutor exportTaskExecutor()
    {
        return boundedExecutor("export", 2, 4, 32, Overflow.ABORT);
    }

    /**
     * AI 池：调用模型接口，时间几乎全耗在阻塞网络 IO 上。
     *
     * <p>并发度与模型侧配额对齐（4~8）：开再多线程也只是把 429 换成超时。当前 AI 调用同样在请求线程内，
     * 此池是异步化的接入点。
     */
    @Bean(name = "aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor()
    {
        return boundedExecutor("ai", 4, 8, 64, Overflow.ABORT);
    }

    /**
     * 审计/异步日志池：操作日志、登录日志等短任务（原 {@code scheduledExecutorService} 的职责）。
     *
     * <p>队列 512 + 回压：这类任务单条毫秒级，正常永远跑不满；真排满时说明数据库写不动了，
     * 此时回压到提交线程比丢弃更好——审计日志不可丢。
     */
    @Bean(name = "asyncLogExecutor")
    public ThreadPoolTaskExecutor asyncLogExecutor()
    {
        return boundedExecutor("async-log", 4, 8, 512, Overflow.BACKPRESSURE);
    }

    /**
     * 通用池：补回被拆池挤掉的 {@code applicationTaskExecutor} 隐式契约。
     *
     * <p>Boot 的 {@code TaskExecutionAutoConfiguration} 给 {@code applicationTaskExecutor} 挂着
     * {@code @ConditionalOnMissingBean(Executor.class)}——本类一旦声明了上面四个池，它就不再创建这个
     * 通用执行器。而 Flowable 的 {@code ProcessEngineAutoConfiguration} 是按名字
     * {@code @Qualifier("applicationTaskExecutor")} 找 AsyncTaskExecutor（异步作业执行器）的，
     * 找不到则整个进程起不来：
     * {@code No qualifying bean of type 'org.springframework.core.task.AsyncTaskExecutor'}。
     * 这是拆池时被连带删掉的隐式契约，必须显式补回，而不是让三方组件去猜别的池。
     *
     * <p>规格按「通用、可回压」定：Flowable 异步作业与未指定池的 {@code @Async} 都落这里，
     * 丢一个就是业务卡单，所以用 BACKPRESSURE 而不是 ABORT。
     */
    @Bean(name = "applicationTaskExecutor")
    @Primary
    public ThreadPoolTaskExecutor applicationTaskExecutor()
    {
        return boundedExecutor("application", 8, 16, 512, Overflow.BACKPRESSURE);
    }

    /** 某池累计拒绝次数（0 表示自启动以来从未拒绝）。 */
    public long rejectedCount(String pool)
    {
        AtomicLong counter = rejections.get(pool);
        return counter == null ? 0L : counter.get();
    }

    /**
     * 构建一个有界、带 MDC 传递、拒绝即计数的执行器。
     *
     * @param poolName 池名（同时作为线程名前缀与指标 tag）
     * @param core     核心线程数
     * @param max      最大线程数
     * @param queue    队列容量（必须 > 0：队列无穷等于没限流）
     * @param overflow 队满时的取舍
     */
    private ThreadPoolTaskExecutor boundedExecutor(String poolName, int core, int max, int queue, Overflow overflow)
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(poolName + "-");
        // V4.0 A5：异步任务携带提交线程的 traceId，否则选课/报盘这类异步链路在日志里断开
        executor.setTaskDecorator(new MdcTaskDecorator(poolName));
        // 停机时把在途任务做完，避免"日志写一半、任务状态停在执行中"
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS);

        OverloadHandler handler = new OverloadHandler(poolName, overflow, rejections);
        executor.setRejectedExecutionHandler(handler);
        bindMetrics(poolName, executor, handler.counter);
        return executor;
    }

    /** 把队列水位/活跃线程/拒绝次数暴露为 micrometer 指标；无常用量表时静默跳过（单测环境）。 */
    private void bindMetrics(String poolName, ThreadPoolTaskExecutor executor, AtomicLong rejectionCounter)
    {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null)
        {
            return;
        }
        Gauge.builder("yu.executor.rejected", rejectionCounter, AtomicLong::doubleValue)
                .description("线程池累计拒绝任务数（T-6）")
                .tag("pool", poolName)
                .register(registry);
        Gauge.builder("yu.executor.queue.size", executor, ThreadPoolConfig::queueSize)
                .description("线程池当前排队任务数")
                .tag("pool", poolName)
                .register(registry);
        Gauge.builder("yu.executor.active", executor, ThreadPoolConfig::activeCount)
                .description("线程池当前活跃线程数")
                .tag("pool", poolName)
                .register(registry);
    }

    /** 排队数；池尚未初始化时返回 -1（Spring 在 bean 初始化后才建真正的 ThreadPoolExecutor）。 */
    private static double queueSize(ThreadPoolTaskExecutor executor)
    {
        try
        {
            return executor.getThreadPoolExecutor().getQueue().size();
        }
        catch (Exception e)
        {
            return -1D;
        }
    }

    private static double activeCount(ThreadPoolTaskExecutor executor)
    {
        try
        {
            return executor.getThreadPoolExecutor().getActiveCount();
        }
        catch (Exception e)
        {
            return -1D;
        }
    }

    /** 队满时的取舍方式：长任务明确失败，审计日志回压不丢。 */
    enum Overflow
    {
        ABORT, BACKPRESSURE
    }

    /**
     * 拒绝处理器：先计数（+限频告警），再按 {@link Overflow} 决定抛出还是回压。
     *
     * <p>告警限频是必须的：一次批量提交可能瞬间产生上万次拒绝，逐条 WARN 会把真正的原因冲掉。
     * 这里首次必打，之后每 100 次打一条，累计值始终可从指标读取。
     */
    static class OverloadHandler implements RejectedExecutionHandler
    {
        private final String poolName;

        private final Overflow overflow;

        private final AtomicLong counter;

        OverloadHandler(String poolName, Overflow overflow, Map<String, AtomicLong> registry)
        {
            this.poolName = poolName;
            this.overflow = overflow;
            this.counter = registry.computeIfAbsent(poolName, key -> new AtomicLong());
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            long total = counter.incrementAndGet();
            if (total == 1L || total % 100L == 0L)
            {
                log.warn("线程池已满，任务被{}：pool={}, 累计={}, 队列容量={}, 最大线程数={}",
                        overflow == Overflow.ABORT ? "拒绝" : "回压到提交线程", poolName, total,
                        executor.getQueue().remainingCapacity() + executor.getQueue().size(),
                        executor.getMaximumPoolSize());
            }
            if (overflow == Overflow.BACKPRESSURE)
            {
                // 审计日志不可丢：宁可让请求慢一点，也不能整条日志消失（线程池已关闭时才是真丢）
                if (executor.isShutdown())
                {
                    log.error("线程池已关闭，回压任务无法执行，审计日志可能缺失：pool={}", poolName);
                    return;
                }
                task.run();
                return;
            }
            throw new RejectedExecutionException(poolName + " 线程池队列已满，任务被拒绝（累计 " + total + " 次）");
        }
    }

    /**
     * MDC 上下文传递装饰器：提交时快照 traceId，执行时恢复，结束后清理（线程复用会串号）。
     * 顺带把逃逸到线程池边界的异常记下来——否则任务在异步线程里炸掉只会变成一条无主堆栈。
     */
    static class MdcTaskDecorator implements TaskDecorator
    {
        private final String poolName;

        MdcTaskDecorator(String poolName)
        {
            this.poolName = poolName;
        }

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
                catch (Throwable t)
                {
                    log.error("异步任务执行异常：pool={}", poolName, t);
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
}
