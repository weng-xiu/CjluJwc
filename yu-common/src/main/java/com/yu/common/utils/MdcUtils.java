package com.yu.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;

/**
 * 链路追踪工具（V4.0 A5 可观测性第一步：traceId 贯穿）
 * <p>
 * traceId 存放于 SLF4J MDC，logback 通过 %X{traceId} 输出到文本日志、
 * JsonEncoder 自动把 MDC 内容写入 JSON 字段，实现"一次请求一个 ID"的串联。
 *
 * @author cjlu
 */
public class MdcUtils
{
    /** MDC 中链路编号的键名（日志与 JSON 字段名一致） */
    public static final String TRACE_ID = "traceId";

    /** 上下游传递 traceId 的 HTTP 头 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 获取当前线程的 traceId，不存在则生成并写入 MDC
     */
    public static String currentOrNew()
    {
        String traceId = MDC.get(TRACE_ID);
        if (StringUtils.isEmpty(traceId))
        {
            traceId = generate();
            MDC.put(TRACE_ID, traceId);
        }
        return traceId;
    }

    /**
     * 直接写入 MDC（入口层已带 traceId 时复用，保持与上游一致）
     */
    public static void put(String traceId)
    {
        if (StringUtils.isEmpty(traceId))
        {
            traceId = generate();
        }
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * 清理 MDC，必须在请求/任务结束时调用，避免线程复用串号
     */
    public static void clear()
    {
        MDC.remove(TRACE_ID);
    }

    /**
     * 快照当前 MDC 上下文，供异步线程恢复使用
     */
    public static Map<String, String> copyContext()
    {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context == null ? new HashMap<>() : context;
    }

    /**
     * 生成 16 位短 traceId（去掉 UUID 横杠，便于日志与工单引用）
     */
    public static String generate()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
