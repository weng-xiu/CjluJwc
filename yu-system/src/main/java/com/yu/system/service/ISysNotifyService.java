package com.yu.system.service;

import java.util.List;
import com.yu.system.domain.SysNotifyLog;

/**
 * 多渠道统一通知服务（S6）。
 * 站内信始终送达；邮件 / 短信渠道由系统参数开关控制，SMTP 与开关均可后台配置。
 * 每个渠道的发送结果均写入 sys_notify_log 留痕，供送达审计。
 */
public interface ISysNotifyService
{
    /**
     * 向指定用户发起多渠道通知。
     *
     * @param receiverUserId 接收人用户ID
     * @param msgType        消息类型（0预警 1审批 2变更 3通知）
     * @param title          标题
     * @param content        正文
     * @param businessType   关联业务类型
     * @param businessId     关联业务ID
     * @param mail           是否尝试邮件渠道（仍受全局开关约束）
     * @param sms            是否尝试短信渠道（仍受全局开关约束）
     */
    void notifyUser(Long receiverUserId, String msgType, String title, String content,
                    String businessType, Long businessId, boolean mail, boolean sms);

    /** 仅站内信通知（等价 notifyUser(..., mail=false, sms=false)） */
    void notifyUserSite(Long receiverUserId, String msgType, String title, String content,
                        String businessType, Long businessId);

    /** 查询渠道发送留痕 */
    List<SysNotifyLog> selectNotifyLogList(SysNotifyLog query);
}
