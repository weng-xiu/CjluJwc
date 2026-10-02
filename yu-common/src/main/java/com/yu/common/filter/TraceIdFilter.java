package com.yu.common.filter;

import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.yu.common.utils.MdcUtils;
import com.yu.common.utils.StringUtils;

/**
 * 链路追踪过滤器（V4.0 A5 可观测性第一步）
 * <p>
 * 为每个请求建立 traceId：优先复用上游（网关/前端）透传的 X-Trace-Id，
 * 写入 MDC 供日志（文本 %X{traceId} / JSON MDC 字段）与操作日志使用，
 * 同时回写响应头，便于前端报错与后端日志一键对齐。请求结束务必清理，避免线程复用串号。
 *
 * @author cjlu
 */
public class TraceIdFilter implements Filter
{
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        // 已进入过本过滤器（如内部 forward/include 二次进入）时不重复生成与清理
        if (MDC.get(MdcUtils.TRACE_ID) != null)
        {
            chain.doFilter(request, response);
            return;
        }
        Map<String, String> parentContext = MdcUtils.copyContext();
        try
        {
            String traceId = resolveTraceId(request);
            MdcUtils.put(traceId);
            if (response instanceof HttpServletResponse)
            {
                ((HttpServletResponse) response).setHeader(MdcUtils.TRACE_ID_HEADER, traceId);
            }
            chain.doFilter(request, response);
        }
        finally
        {
            MdcUtils.clear();
            // 恢复父上下文（本过滤器可能运行在已带 MDC 的异步线程中）
            parentContext.forEach(MDC::put);
        }
    }

    /**
     * 取上游透传的 traceId，无则生成；对透传值做长度与字符收敛，避免日志注入
     */
    private String resolveTraceId(ServletRequest request)
    {
        if (request instanceof HttpServletRequest)
        {
            String incoming = ((HttpServletRequest) request).getHeader(MdcUtils.TRACE_ID_HEADER);
            if (StringUtils.isNotEmpty(incoming))
            {
                String sanitized = incoming.replaceAll("[^A-Za-z0-9\\-]", "");
                if (StringUtils.isNotEmpty(sanitized))
                {
                    return sanitized.length() > 32 ? sanitized.substring(0, 32) : sanitized;
                }
            }
        }
        return MdcUtils.generate();
    }
}
