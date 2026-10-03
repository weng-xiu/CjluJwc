package com.yu.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务埋点注解（Q3 可观测性）
 * <p>
 * 标注在核心业务方法上，由 {@code BizMetricAspect} 环绕拦截，自动记录：
 * <ul>
 *   <li>调用耗时直方图（timer：yu.biz.duration，tag=event）——支撑选课尖峰响应、审核时效等 P95/P99 观测；</li>
 *   <li>调用次数与成败分布（counter：yu.biz.total，tag=event/result）——支撑每秒操作数（速率）与成功率观测。</li>
 * </ul>
 * 采用注解 + AOP 而非方法体内直调，遵循"新增不动旧"：不侵入既有业务方法体与事务边界，
 * 仅在方法签名上追加一个标记注解即可接入指标。指标成败判定基于返回值：
 * {@code AjaxResult} 读取 code==200 视为成功；抛出异常视为失败；其余返回类型按是否异常判定。
 *
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BizMetric
{
    /**
     * 业务事件名（作为指标 tag，如 selection/approval）
     */
    public String value();
}
