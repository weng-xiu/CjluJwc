package com.yu.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysNotifyLog;
import com.yu.system.mapper.SysNotifyLogMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.notify.SmsSender;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysNotifyService;

/**
 * 多渠道统一通知服务实现（S6）。
 * 站内信走 SysMessage（P1 消息中心）恒送达；邮件按 sys_config 中 SMTP 配置构建发送器；
 * 短信经 SmsSender 抽象（默认仅留痕，网关待对接）。任一渠道失败均不影响主流程与其它渠道。
 */
@Service
public class SysNotifyServiceImpl implements ISysNotifyService
{
    private static final Logger log = LoggerFactory.getLogger(SysNotifyServiceImpl.class);

    /** 渠道标识 */
    private static final String CH_SITE = "site";
    private static final String CH_MAIL = "mail";
    private static final String CH_SMS = "sms";

    /** 发送状态 */
    private static final String ST_SUCCESS = "0";
    private static final String ST_FAIL = "1";
    private static final String ST_SKIP = "2";

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SmsSender smsSender;

    @Autowired
    private SysNotifyLogMapper sysNotifyLogMapper;

    @Override
    public void notifyUserSite(Long receiverUserId, String msgType, String title, String content,
                               String businessType, Long businessId)
    {
        notifyUser(receiverUserId, msgType, title, content, businessType, businessId, false, false);
    }

    @Override
    public void notifyUser(Long receiverUserId, String msgType, String title, String content,
                           String businessType, Long businessId, boolean mail, boolean sms)
    {
        if (receiverUserId == null)
        {
            return;
        }
        SysUser user = null;
        try
        {
            user = sysUserMapper.selectUserById(receiverUserId);
        }
        catch (Exception e)
        {
            log.error("通知服务查询接收人失败 receiverId={}", receiverUserId, e);
        }

        // 1) 站内信：恒送达
        sendSite(receiverUserId, msgType, title, content, businessType, businessId);

        // 2) 邮件渠道
        if (mail && isEnable("sys.notify.mail.enabled"))
        {
            String email = user == null ? null : user.getEmail();
            sendMail(receiverUserId, email, title, content, businessType, businessId);
        }

        // 3) 短信渠道
        if (sms && isEnable("sys.notify.sms.enabled"))
        {
            String phone = user == null ? null : user.getPhonenumber();
            sendSms(receiverUserId, phone, title, content, businessType, businessId);
        }
    }

    @Override
    public List<SysNotifyLog> selectNotifyLogList(SysNotifyLog query)
    {
        return sysNotifyLogMapper.selectSysNotifyLogList(query);
    }

    // ================= 渠道实现 =================

    private void sendSite(Long receiverUserId, String msgType, String title, String content,
                          String businessType, Long businessId)
    {
        try
        {
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverUserId);
            msg.setMsgType(msgType);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(businessType);
            msg.setBusinessId(businessId);
            msg.setCreateBy("system");
            sysMessageService.sendMessage(msg);
            writeLog(CH_SITE, receiverUserId, "site:" + receiverUserId, businessType, businessId, title, ST_SUCCESS, null);
        }
        catch (Exception e)
        {
            log.error("站内信发送失败 receiverId={} title={}", receiverUserId, title, e);
            writeLog(CH_SITE, receiverUserId, "site:" + receiverUserId, businessType, businessId, title, ST_FAIL, e.getMessage());
        }
    }

    private void sendMail(Long receiverUserId, String email, String title, String content,
                          String businessType, Long businessId)
    {
        if (StringUtils.isEmpty(email))
        {
            writeLog(CH_MAIL, receiverUserId, null, businessType, businessId, title, ST_SKIP, "接收人邮箱为空");
            return;
        }
        try
        {
            JavaMailSenderImpl sender = buildMailSender();
            if (sender == null)
            {
                writeLog(CH_MAIL, receiverUserId, email, businessType, businessId, title, ST_SKIP, "SMTP未配置(sys.notify.mail.host)");
                return;
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            String from = config("sys.notify.mail.from");
            mail.setFrom(StringUtils.isNotEmpty(from) ? from : sender.getUsername());
            mail.setTo(email);
            mail.setSubject(title);
            mail.setText(content);
            sender.send(mail);
            writeLog(CH_MAIL, receiverUserId, email, businessType, businessId, title, ST_SUCCESS, null);
        }
        catch (Exception e)
        {
            log.error("邮件发送失败 to={} title={}", email, title, e);
            writeLog(CH_MAIL, receiverUserId, email, businessType, businessId, title, ST_FAIL, e.getMessage());
        }
    }

    private void sendSms(Long receiverUserId, String phone, String title, String content,
                         String businessType, Long businessId)
    {
        if (StringUtils.isEmpty(phone))
        {
            writeLog(CH_SMS, receiverUserId, null, businessType, businessId, title, ST_SKIP, "接收人手机号为空");
            return;
        }
        try
        {
            boolean ok = smsSender.send(phone, title + "：" + content);
            writeLog(CH_SMS, receiverUserId, phone, businessType, businessId, title,
                    ok ? ST_SUCCESS : ST_FAIL, ok ? null : "网关返回失败");
        }
        catch (Exception e)
        {
            log.error("短信发送失败 to={} title={}", phone, title, e);
            writeLog(CH_SMS, receiverUserId, phone, businessType, businessId, title, ST_FAIL, e.getMessage());
        }
    }

    // ================= 辅助 =================

    private JavaMailSenderImpl buildMailSender()
    {
        String host = config("sys.notify.mail.host");
        if (StringUtils.isEmpty(host))
        {
            return null;
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(parseInt(config("sys.notify.mail.port"), 25));
        sender.setUsername(config("sys.notify.mail.username"));
        sender.setPassword(config("sys.notify.mail.password"));
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        if (isEnable("sys.notify.mail.ssl"))
        {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }
        return sender;
    }

    private void writeLog(String channel, Long receiverId, String target, String businessType,
                          Long businessId, String title, String status, String errorMsg)
    {
        try
        {
            SysNotifyLog log = new SysNotifyLog();
            log.setChannel(channel);
            log.setReceiverId(receiverId);
            log.setTarget(target);
            log.setBusinessType(businessType);
            log.setBusinessId(businessId);
            log.setTitle(title);
            log.setStatus(status);
            if (StringUtils.isNotEmpty(errorMsg))
            {
                log.setErrorMsg(errorMsg.length() > 480 ? errorMsg.substring(0, 480) : errorMsg);
            }
            Date now = DateUtils.getNowDate();
            log.setSendTime(now);
            log.setCreateTime(now);
            sysNotifyLogMapper.insertSysNotifyLog(log);
        }
        catch (Exception e)
        {
            SysNotifyServiceImpl.log.error("写入通知留痕失败 channel={}", channel, e);
        }
    }

    private String config(String key)
    {
        String v = configService.selectConfigByKey(key);
        return StringUtils.trim(v);
    }

    private boolean isEnable(String key)
    {
        return "true".equalsIgnoreCase(config(key));
    }

    private int parseInt(String v, int def)
    {
        try
        {
            return StringUtils.isEmpty(v) ? def : Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e)
        {
            return def;
        }
    }
}
