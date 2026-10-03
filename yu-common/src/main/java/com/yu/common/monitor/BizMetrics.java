package com.yu.common.monitor;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 业务埋点门面（Q3 可观测性）
 * <p>
 * 统一封装对 Micrometer {@link MeterRegistry} 的写入，向 Prometheus 暴露三类核心业务指标：
 * <ul>
 *   <li>{@code yu.biz.duration}（Timer，tag=event）：选课/审核等关键动作的耗时分布，支撑响应时延与尖峰观测；</li>
 *   <li>{@code yu.biz.total}（Counter，tag=event,result）：关键动作调用次数与成败计数，配合 rate() 得到 QPS/成功率；</li>
 *   <li>{@code yu.warning.triggered}（Counter，tag=type,level）：学业预警触发计数，支撑预警量与分级分布看板。</li>
 * </ul>
 * 置于 yu-common 以便全部业务模块（tpm/sam/…）注入使用；指标命名以点号分隔，
 * Micrometer 导出到 Prometheus 时自动转为下划线（yu_biz_duration_seconds 等）。
 *
 * @author ruoyi
 */
@Component
public class BizMetrics
{
    /** 关键动作耗时（秒） */
    public static final String TIMER_BIZ = "yu.biz.duration";
    /** 关键动作调用计数 */
    public static final String COUNTER_BIZ = "yu.biz.total";
    /** 预警触发计数 */
    public static final String COUNTER_WARNING = "yu.warning.triggered";

    /** 结果 tag 常量 */
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_FAIL = "fail";

    private final MeterRegistry registry;

    public BizMetrics(MeterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * 记录一次关键业务动作（耗时 + 成败计数）。
     *
     * @param event    事件名（如 selection / approval）
     * @param success  是否成功
     * @param duration 耗时
     */
    public void record(String event, boolean success, Duration duration)
    {
        if (event == null || event.isEmpty())
        {
            return;
        }
        Timer.builder(TIMER_BIZ)
                .tag("event", event)
                .description("关键业务动作耗时")
                .publishPercentileHistogram()
                .register(registry)
                .record(duration == null ? Duration.ZERO : duration);

        Counter.builder(COUNTER_BIZ)
                .tag("event", event)
                .tag("result", success ? RESULT_SUCCESS : RESULT_FAIL)
                .description("关键业务动作调用次数（按成败）")
                .register(registry)
                .increment();
    }

    /**
     * 记录一次学业预警触发。
     *
     * @param type  预警类型（如 1学业 2出勤 3综合，允许为空则记 unknown）
     * @param level 预警级别（如 0一般 1严重 2高危，允许为空则记 unknown）
     */
    public void recordWarning(String type, String level)
    {
        Counter.builder(COUNTER_WARNING)
                .tag("type", type == null ? "unknown" : type)
                .tag("level", level == null ? "unknown" : level)
                .description("学业预警触发次数（按类型与级别）")
                .register(registry)
                .increment();
    }
}
