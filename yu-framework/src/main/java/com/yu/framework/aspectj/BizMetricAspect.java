package com.yu.framework.aspectj;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yu.common.annotation.BizMetric;
import com.yu.common.constant.HttpStatus;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.monitor.BizMetrics;

/**
 * 业务埋点切面（Q3 可观测性）
 * <p>
 * 环绕 {@link BizMetric} 标注的方法，记录耗时与成败并写入 {@link BizMetrics}。
 * 与业务代码解耦：只需在方法上加注解即可接入指标，不改方法体、不影响事务边界。
 * 埋点自身的任何异常都被吞掉并降级为日志，绝不影响主业务流程。
 *
 * @author ruoyi
 */
@Aspect
@Component
public class BizMetricAspect
{
    private static final Logger log = LoggerFactory.getLogger(BizMetricAspect.class);

    @Autowired
    private BizMetrics bizMetrics;

    @Around("@annotation(bizMetric)")
    public Object around(ProceedingJoinPoint joinPoint, BizMetric bizMetric) throws Throwable
    {
        long start = System.nanoTime();
        boolean success = false;
        try
        {
            Object result = joinPoint.proceed();
            success = resolveSuccess(result);
            return result;
        }
        catch (Throwable ex)
        {
            // 抛出异常一律计为失败，原异常继续上抛，不改变既有语义
            success = false;
            throw ex;
        }
        finally
        {
            try
            {
                Duration cost = Duration.ofNanos(System.nanoTime() - start);
                bizMetrics.record(bizMetric.value(), success, cost);
            }
            catch (Exception e)
            {
                // 指标写入失败不应影响主流程
                log.debug("业务埋点记录失败 event={}", bizMetric.value(), e);
            }
        }
    }

    /**
     * 依据返回值判定成功：AjaxResult 读取 code==200；其余返回类型视为成功（异常路径已在 catch 中计失败）。
     */
    private boolean resolveSuccess(Object result)
    {
        if (result instanceof AjaxResult)
        {
            Object code = ((AjaxResult) result).get(AjaxResult.CODE_TAG);
            return code != null && Integer.valueOf(HttpStatus.SUCCESS).equals(toInt(code));
        }
        return true;
    }

    private Integer toInt(Object code)
    {
        try
        {
            return code instanceof Number ? ((Number) code).intValue() : Integer.valueOf(String.valueOf(code));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
