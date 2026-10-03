package com.yu.quartz.util;

import java.util.Date;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysNotice;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysNoticeService;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * 指标告警服务（Q3 可观测性 - 告警闭环）
 * <p>
 * 在既有 {@link JobFailureAlertService}（仅覆盖定时任务失败）之外，补齐"资源水位"与"接口错误率"
 * 两类运行态告警：由定时任务 {@code resourceAlertTask} 周期性驱动，从 Micrometer {@link MeterRegistry}
 * 读取 JVM 堆水位与 HTTP 5xx 错误率，超过阈值即向系统通知公告(sys_notice)写入一条告警，
 * 复用现有通知通道，使运维可在管理端"通知公告"页及时感知故障。
 * <p>
 * 阈值全部来自 sys_config（可运维调整、免重启）；同一指标设冷却期，避免高频调度刷屏。
 *
 * @author ruoyi
 */
@Component
public class MetricsAlertService
{
    private static final Logger log = LoggerFactory.getLogger(MetricsAlertService.class);

    /** 通知标题最大长度（sys_notice.notice_title 受 Xss 校验限制为 50 字符） */
    private static final int TITLE_MAX_LENGTH = 50;

    /** 参数键：堆内存使用率告警阈值（百分比） */
    public static final String CFG_HEAP_PERCENT = "sys.observability.alert.heapPercent";
    /** 参数键：HTTP 5xx 错误率告警阈值（百分比） */
    public static final String CFG_HTTP_ERROR_RATE = "sys.observability.alert.httpErrorRatePercent";
    /** 参数键：错误率判定所需最小样本请求数（样本过小不告警，避免抖动误报） */
    public static final String CFG_HTTP_MIN_REQUESTS = "sys.observability.alert.httpMinRequests";
    /** 参数键：同一指标告警冷却分钟数（防刷屏） */
    public static final String CFG_COOLDOWN_MINUTES = "sys.observability.alert.cooldownMinutes";

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysNoticeService noticeService;

    /** 各指标最近一次告警时间戳（毫秒），用于冷却去重 */
    private final Map<String, Long> lastAlertAt = new ConcurrentHashMap<>();

    /**
     * 执行一轮水位与错误率检查，超阈值则发送通知。
     *
     * @return 本轮触发的告警条数
     */
    public int checkAndAlert()
    {
        int alerts = 0;
        alerts += checkHeap();
        alerts += checkHttpErrorRate();
        if (alerts == 0)
        {
            log.debug("指标告警检查完成，未触发任何阈值");
        }
        return alerts;
    }

    /** JVM 堆水位检查 */
    private int checkHeap()
    {
        double used = sumGauge("jvm.memory.used", "area", "heap");
        double max = sumGauge("jvm.memory.max", "area", "heap");
        // max <= 0 表示堆未设上限（-Xmx 未固定），不做百分比判定
        if (used <= 0 || max <= 0)
        {
            return 0;
        }
        double percent = used / max * 100.0;
        double threshold = readDouble(CFG_HEAP_PERCENT, 85.0);
        if (percent >= threshold && allowAlert("heap", 0))
        {
            String content = "<p><b>资源水位告警：JVM 堆内存使用率过高</b></p>"
                    + "<p>当前使用率：" + format(percent) + "%（阈值 " + format(threshold) + "%）</p>"
                    + "<p>已用：" + mb(used) + " MB / 上限：" + mb(max) + " MB</p>"
                    + "<p>检测时间：" + new Date() + "</p>"
                    + "<p style=\"color:#F56C6C;\">请运维人员关注内存趋势，必要时扩容或排查内存泄漏。</p>";
            sendAlert("[告警] JVM堆内存使用率 " + format(percent) + "%", content);
            return 1;
        }
        return 0;
    }

    /** HTTP 5xx 错误率检查 */
    private int checkHttpErrorRate()
    {
        long total = 0L;
        long serverError = 0L;
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        for (Timer t : timers)
        {
            long c = (long) t.count();
            total += c;
            String outcome = t.getId().getTag("outcome");
            if ("SERVER_ERROR".equals(outcome))
            {
                serverError += c;
            }
        }
        long minRequests = (long) readDouble(CFG_HTTP_MIN_REQUESTS, 50.0);
        if (total < minRequests)
        {
            // 累计样本不足，暂不判定（http.server.requests 为进程生命周期累计计数）
            return 0;
        }
        double rate = serverError * 100.0 / total;
        double threshold = readDouble(CFG_HTTP_ERROR_RATE, 5.0);
        if (rate >= threshold && allowAlert("http5xx", 0))
        {
            String content = "<p><b>接口错误率告警：HTTP 5xx 占比过高</b></p>"
                    + "<p>累计错误率：" + format(rate) + "%（阈值 " + format(threshold) + "%）</p>"
                    + "<p>5xx 次数：" + serverError + " / 总请求：" + total + "</p>"
                    + "<p>检测时间：" + new Date() + "</p>"
                    + "<p style=\"color:#F56C6C;\">请排查服务端异常日志与近期发布变更。</p>";
            sendAlert("[告警] HTTP 5xx 错误率 " + format(rate) + "%", content);
            return 1;
        }
        return 0;
    }

    private double sumGauge(String name, String tagKey, String tagValue)
    {
        double sum = 0.0;
        for (Gauge g : meterRegistry.find(name).tag(tagKey, tagValue).gauges())
        {
            double v = g.value();
            if (v > 0)
            {
                sum += v;
            }
        }
        return sum;
    }

    /**
     * 冷却判定：同一 key 距上次告警超过冷却期才允许再次告警，并刷新时间戳。
     */
    private boolean allowAlert(String key, long ignored)
    {
        long cooldownMs = (long) readDouble(CFG_COOLDOWN_MINUTES, 15.0) * 60_000L;
        long now = System.currentTimeMillis();
        Long last = lastAlertAt.get(key);
        if (last != null && now - last < cooldownMs)
        {
            return false;
        }
        lastAlertAt.put(key, now);
        return true;
    }

    private void sendAlert(String title, String content)
    {
        try
        {
            String safeTitle = title.length() > TITLE_MAX_LENGTH ? title.substring(0, TITLE_MAX_LENGTH) : title;
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle(safeTitle);
            notice.setNoticeType("1"); // 1=通知
            notice.setNoticeContent(content);
            notice.setStatus("0"); // 0=正常
            notice.setCreateBy("monitor");
            notice.setCreateTime(new Date());
            noticeService.insertNotice(notice);
            log.warn("指标告警已发送：{}", safeTitle);
        }
        catch (Exception e)
        {
            // 告警自身失败不应影响主流程，仅记录日志
            log.error("发送指标告警时出现异常：{}", e.getMessage(), e);
        }
    }

    private double readDouble(String key, double def)
    {
        try
        {
            String v = configService.selectConfigByKey(key);
            if (StringUtils.isNotEmpty(v))
            {
                return Double.parseDouble(v.trim());
            }
        }
        catch (Exception e)
        {
            log.debug("读取告警阈值参数失败 key={}", key, e);
        }
        return def;
    }

    private double mb(double bytes)
    {
        return round(bytes / 1024.0 / 1024.0);
    }

    private double format(double v)
    {
        return round(v);
    }

    private double round(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }
}
