package com.yu.quartz.util;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.system.domain.SysNotice;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysNoticeService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MetricsAlertService 单元测试（V5.0 §7.4 P2 残项：抽签耗时 / 超卖尝试告警）。
 * <p>
 * 用 SimpleMeterRegistry 直接装配业务埋点指标（yu.biz.duration{event=lottery}、
 * yu.selection.oversell），mock ISysConfigService 提供阈值、ISysNoticeService 捕获告警写入，
 * 覆盖：达阈值告警、未达阈值不告警、冷却去重、超卖本轮窗口增量与累计基线推进。
 * 其余检查（堆/HTTP）在空注册表下自然返回 0，不影响断言。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetricsAlertServiceTest
{
    @Mock private ISysConfigService configService;
    @Mock private ISysNoticeService noticeService;

    private SimpleMeterRegistry registry;
    private MetricsAlertService service;

    @BeforeEach
    void setUp()
    {
        registry = new SimpleMeterRegistry();
        service = new MetricsAlertService();
        ReflectionTestUtils.setField(service, "meterRegistry", registry);
        ReflectionTestUtils.setField(service, "configService", configService);
        ReflectionTestUtils.setField(service, "noticeService", noticeService);
    }

    @Test
    @DisplayName("抽签耗时：最大耗时达阈值即告警一条")
    void lotteryLatency_overThreshold_alerts()
    {
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_LOTTERY_MAX_SECONDS))).thenReturn("1");
        Timer.builder("yu.biz.duration").tag("event", "lottery")
                .publishPercentileHistogram().register(registry)
                .record(Duration.ofSeconds(2));

        int alerts = service.checkAndAlert();

        assertEquals(1, alerts, "抽签最大耗时 2s ≥ 阈值 1s，应触发 1 条告警");
        verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("抽签耗时：未达阈值不告警")
    void lotteryLatency_underThreshold_noAlert()
    {
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_LOTTERY_MAX_SECONDS))).thenReturn("30");
        Timer.builder("yu.biz.duration").tag("event", "lottery")
                .publishPercentileHistogram().register(registry)
                .record(Duration.ofSeconds(5));

        int alerts = service.checkAndAlert();

        assertEquals(0, alerts, "抽签耗时 5s < 阈值 30s，不应告警");
        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("抽签耗时：无抽签样本（count=0）不判定")
    void lotteryLatency_noSample_noAlert()
    {
        // 注册表无任何 lottery timer
        int alerts = service.checkAndAlert();
        assertEquals(0, alerts);
        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("超卖尝试：本轮窗口增量达阈值即告警，并推进累计基线")
    void oversell_reachesThreshold_alertsOnce()
    {
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_OVERSELL_COUNT))).thenReturn("20");
        Counter.builder("yu.selection.oversell").register(registry).increment(25);

        // 首轮：delta = 25 - 0 = 25 ≥ 20 → 告警
        assertEquals(1, service.checkAndAlert(), "本轮增量 25 达阈值 20，应告警");
        // 次轮：累计仍 25、基线已推进至 25 → delta 0 → 不重复告警
        assertEquals(0, service.checkAndAlert(), "无新增超卖时不应重复告警（基线已推进）");

        verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("超卖尝试：增量未达阈值不告警")
    void oversell_underThreshold_noAlert()
    {
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_OVERSELL_COUNT))).thenReturn("20");
        Counter.builder("yu.selection.oversell").register(registry).increment(5);

        int alerts = service.checkAndAlert();

        assertEquals(0, alerts, "增量 5 < 阈值 20，不应告警");
        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("冷却去重：同一指标冷却期内第二次不告警")
    void sameMetric_cooldown_dedupes()
    {
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_OVERSELL_COUNT))).thenReturn("1");
        when(configService.selectConfigByKey(eq(MetricsAlertService.CFG_COOLDOWN_MINUTES))).thenReturn("60");
        Counter.builder("yu.selection.oversell").register(registry).increment(2);

        assertEquals(1, service.checkAndAlert(), "首次达阈值告警");
        Counter.builder("yu.selection.oversell").register(registry).increment(2); // 累计 4，delta 2 ≥ 1
        assertEquals(0, service.checkAndAlert(), "仍在 60 分钟冷却期内，第二次应被去重");

        verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
    }
}
