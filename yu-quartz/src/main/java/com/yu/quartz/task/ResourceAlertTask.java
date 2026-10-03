package com.yu.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yu.quartz.util.MetricsAlertService;

/**
 * 运行态指标告警定时任务（Q3 可观测性 - 告警闭环）
 * <p>
 * 由 Quartz 调度，通过若依定时任务管理页面配置（sys_job 表）。
 * 调用目标：resourceAlertTask.check()
 * <p>
 * 周期性读取 JVM 堆水位与 HTTP 5xx 错误率，超阈值则写入系统通知公告，
 * 与既有 JobFailureAlertService（任务失败告警）共同构成运行态告警闭环。
 * 异常不在此处捕获，向上抛出由 AbstractQuartzJob 统一记录失败日志。
 *
 * @author ruoyi
 */
@Component("resourceAlertTask")
public class ResourceAlertTask
{
    private static final Logger log = LoggerFactory.getLogger(ResourceAlertTask.class);

    @Autowired
    private MetricsAlertService metricsAlertService;

    /**
     * 执行一轮指标告警检查（建议每 5 分钟执行一次）。
     */
    public void check()
    {
        log.info("运行态指标告警检查开始");
        int alerts = metricsAlertService.checkAndAlert();
        log.info("运行态指标告警检查完成，本轮触发告警 {} 条", alerts);
    }
}
