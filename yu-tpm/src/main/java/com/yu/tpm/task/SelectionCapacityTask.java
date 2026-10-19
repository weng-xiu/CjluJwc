package com.yu.tpm.task;

import java.util.Date;
import java.util.List;
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
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.OfferingCapacityStat;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;

/**
 * 选课容量水位告警定时任务（V5.0 §7.4 P2 / 债务清单容量可感知项）。
 * <p>
 * 由 Quartz 调度，通过若依定时任务管理页面配置（sys_job 表）。
 * 调用目标：selectionCapacityTask.check()
 * <p>
 * 周期性扫描"进行中"的选课轮次，统计各开课的已选中人数占容量比例：
 * 满员或达到水位阈值（参数 tpm.selection.alert.capacityPercent，默认 90%）即写系统通知公告，
 * 与 resourceAlertTask（JVM/HTTP 指标告警）共同构成运行态告警闭环；
 * 同轮次告警按冷却期去重（参数 tpm.selection.alert.cooldownMinutes，默认 30 分钟）防刷屏。
 * 超卖防护仍由下游原子扣减 + 分布式锁兜底，本任务只负责"接近上限可感知"。
 *
 * @author ruoyi
 */
@Component("selectionCapacityTask")
public class SelectionCapacityTask
{
    private static final Logger log = LoggerFactory.getLogger(SelectionCapacityTask.class);

    /** 通知标题最大长度（sys_notice.notice_title 受 Xss 校验限制为 50 字符） */
    private static final int TITLE_MAX_LENGTH = 50;

    /** 明细列表最多展示条数（防通知内容过长） */
    private static final int DETAIL_LIMIT = 10;

    /** 参数键：开课容量使用率告警阈值（百分比） */
    public static final String CFG_CAPACITY_PERCENT = "tpm.selection.alert.capacityPercent";

    /** 参数键：同一轮次告警冷却分钟数（防刷屏） */
    public static final String CFG_COOLDOWN_MINUTES = "tpm.selection.alert.cooldownMinutes";

    @Autowired
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysNoticeService noticeService;

    /** 各轮次最近一次告警时间戳（毫秒），用于冷却去重 */
    private final Map<Long, Long> lastAlertAt = new ConcurrentHashMap<>();

    /**
     * 执行一轮选课容量水位检查（建议每 5 分钟执行一次）。
     *
     * @return 本轮触发的告警条数
     */
    public int check()
    {
        TpmSelectionRound query = new TpmSelectionRound();
        query.setRoundStatus("1"); // 1=进行中
        query.setStatus("0");      // 0=正常
        List<TpmSelectionRound> rounds = tpmSelectionRoundMapper.selectTpmSelectionRoundList(query);
        if (rounds == null || rounds.isEmpty())
        {
            log.debug("选课容量水位检查：当前无进行中轮次");
            return 0;
        }
        double threshold = readDouble(CFG_CAPACITY_PERCENT, 90.0);
        int alerts = 0;
        for (TpmSelectionRound round : rounds)
        {
            alerts += checkRound(round, threshold);
        }
        if (alerts == 0)
        {
            log.debug("选课容量水位检查完成，未触发任何阈值");
        }
        return alerts;
    }

    /** 单个轮次的水位检查：满员/接近满员开课达到条件且过冷却期则发通知 */
    private int checkRound(TpmSelectionRound round, double threshold)
    {
        List<OfferingCapacityStat> stats = tpmCourseOfferingMapper.selectRoundCapacityStats(round.getRoundId());
        if (stats == null || stats.isEmpty())
        {
            return 0;
        }
        int full = 0;
        int nearFull = 0;
        long waitlisted = 0L;
        StringBuilder detail = new StringBuilder();
        int shown = 0;
        for (OfferingCapacityStat s : stats)
        {
            if (s.getWaitlisted() != null)
            {
                waitlisted += s.getWaitlisted();
            }
            Integer max = s.getMaxStudents();
            Integer enrolled = s.getEnrolled();
            if (max == null || max <= 0 || enrolled == null)
            {
                continue; // 未设容量上限的开课不参与水位判定
            }
            boolean isFull = enrolled >= max;
            boolean isNear = !isFull && enrolled * 100.0 / max >= threshold;
            if (isFull)
            {
                full++;
            }
            else if (isNear)
            {
                nearFull++;
            }
            else
            {
                continue;
            }
            if (shown < DETAIL_LIMIT)
            {
                detail.append("<li>").append(StringUtils.nvl(s.getCourseName(), "开课#" + s.getOfferingId()))
                        .append("（").append(StringUtils.nvl(s.getTeacherName(), "-")).append("）：")
                        .append(enrolled).append(" / ").append(max)
                        .append("（").append(format(enrolled * 100.0 / max)).append("%）")
                        .append(isFull ? " 已满" : " 接近上限").append("</li>");
                shown++;
            }
        }
        if (full + nearFull == 0)
        {
            return 0;
        }
        if (!allowAlert(round.getRoundId()))
        {
            log.debug("选课容量告警处于冷却期，跳过轮次 {}", round.getRoundId());
            return 0;
        }
        String roundName = StringUtils.nvl(round.getRoundName(), "轮次#" + round.getRoundId());
        String content = "<p><b>选课容量水位告警：" + roundName + "</b></p>"
                + "<p>统计开课数：" + stats.size() + "；已满：" + full
                + "；达到阈值(" + format(threshold) + "%)未满：" + nearFull
                + "；候补总人数：" + waitlisted + "</p>"
                + "<p>水位明细（按已选人数降序，最多 " + DETAIL_LIMIT + " 条）：</p><ul>" + detail + "</ul>"
                + "<p>检测时间：" + new Date() + "</p>"
                + "<p style=\"color:#F56C6C;\">请关注热门课程是否需要扩容（弹性容量 elastic_max）或增开班次；超卖已由原子扣减与分布式锁兜底。</p>";
        sendAlert("[告警] 选课容量水位：" + roundName + "（已满" + full + "/临界" + nearFull + "）", content);
        return 1;
    }

    /**
     * 冷却判定：同一轮次距上次告警超过冷却期才允许再次告警，并刷新时间戳。
     */
    private boolean allowAlert(Long roundId)
    {
        long cooldownMs = (long) readDouble(CFG_COOLDOWN_MINUTES, 30.0) * 60_000L;
        long now = System.currentTimeMillis();
        Long last = lastAlertAt.get(roundId);
        if (last != null && now - last < cooldownMs)
        {
            return false;
        }
        lastAlertAt.put(roundId, now);
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
            notice.setStatus("0");     // 0=正常
            notice.setCreateBy("monitor");
            notice.setCreateTime(new Date());
            noticeService.insertNotice(notice);
            log.warn("选课容量水位告警已发送：{}", safeTitle);
        }
        catch (Exception e)
        {
            // 告警自身失败不应影响主流程，仅记录日志
            log.error("发送选课容量水位告警时出现异常：{}", e.getMessage(), e);
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
            log.debug("读取选课告警参数失败 key={}", key, e);
        }
        return def;
    }

    private double format(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }
}
