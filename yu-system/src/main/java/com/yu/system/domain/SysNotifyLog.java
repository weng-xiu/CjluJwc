package com.yu.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 消息渠道发送留痕对象 sys_notify_log（S6 多渠道通知）
 * 记录站内信 / 邮件 / 短信各渠道的发送结果，用于送达审计与失败排查。
 */
public class SysNotifyLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 留痕ID */
    private Long logId;

    /** 渠道（site站内信 mail邮件 sms短信） */
    private String channel;

    /** 接收人用户ID */
    private Long receiverId;

    /** 送达目标（邮箱地址 / 手机号 / 站内信为登录名） */
    private String target;

    /** 关联业务类型 */
    private String businessType;

    /** 关联业务ID */
    private Long businessId;

    /** 消息标题 */
    private String title;

    /** 发送状态（0成功 1失败 2跳过-目标缺失或渠道关闭） */
    private String status;

    /** 失败/跳过原因 */
    private String errorMsg;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public Date getSendTime() { return sendTime; }
    public void setSendTime(Date sendTime) { this.sendTime = sendTime; }
}
