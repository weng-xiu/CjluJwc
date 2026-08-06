package com.yu.quartz.util;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yu.common.utils.StringUtils;
import com.yu.quartz.domain.SysJob;
import com.yu.system.domain.SysNotice;
import com.yu.system.service.ISysNoticeService;

/**
 * 定时任务失败告警服务
 *
 * 当关键定时任务执行失败时，向系统通知公告表(sys_notice)写入一条告警通知，
 * 便于管理员在"通知公告"页面及时感知并处理故障。
 *
 * @author ruoyi
 * @date 2026-08-05
 */
@Component
public class JobFailureAlertService
{
    private static final Logger log = LoggerFactory.getLogger(JobFailureAlertService.class);

    /** 通知标题最大长度（sys_notice.notice_title 受 Xss 校验限制为 50 字符） */
    private static final int TITLE_MAX_LENGTH = 50;

    /** 异常信息最大留存长度 */
    private static final int ERROR_MAX_LENGTH = 1000;

    @Autowired
    private ISysNoticeService noticeService;

    /**
     * 发送任务失败告警通知
     *
     * @param sysJob    失败的任务对象
     * @param errorMsg  异常信息
     */
    public void sendJobFailureAlert(SysJob sysJob, String errorMsg)
    {
        try
        {
            String jobName = sysJob.getJobName();
            String title = "[任务告警] " + jobName + " 执行失败";
            if (title.length() > TITLE_MAX_LENGTH)
            {
                title = title.substring(0, TITLE_MAX_LENGTH);
            }

            StringBuilder content = new StringBuilder();
            content.append("<p><b>定时任务执行失败告警</b></p>");
            content.append("<p>任务名称：").append(jobName).append("</p>");
            content.append("<p>任务分组：").append(sysJob.getJobGroup()).append("</p>");
            content.append("<p>调用目标：").append(sysJob.getInvokeTarget()).append("</p>");
            content.append("<p>失败时间：").append(new Date()).append("</p>");
            if (StringUtils.isNotEmpty(errorMsg))
            {
                String safeError = errorMsg.length() > ERROR_MAX_LENGTH
                        ? errorMsg.substring(0, ERROR_MAX_LENGTH) : errorMsg;
                content.append("<p>异常信息：").append(safeError).append("</p>");
            }
            content.append("<p style=\"color:#F56C6C;\">请运维人员及时排查处理。</p>");

            SysNotice notice = new SysNotice();
            notice.setNoticeTitle(title);
            notice.setNoticeType("1"); // 1=通知
            notice.setNoticeContent(content.toString());
            notice.setStatus("0"); // 0=正常
            notice.setCreateBy("scheduler");
            notice.setCreateTime(new Date());
            noticeService.insertNotice(notice);

            log.warn("定时任务失败告警已发送：{}", title);
        }
        catch (Exception e)
        {
            // 告警自身失败不应影响主流程，仅记录日志
            log.error("发送定时任务失败告警时出现异常：{}", e.getMessage(), e);
        }
    }
}
