package com.yu.system.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认短信发送实现：仅记录日志留痕，不真实下发（学校短信网关待对接）。
 * 使多渠道通知链路完整可用，接入真实网关时以 @Primary 覆盖本实现即可。
 */
@Component
public class LoggingSmsSender implements SmsSender
{
    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public boolean send(String phone, String content)
    {
        log.info("[短信留痕-网关未对接] to={} content={}", phone, content);
        return true;
    }
}
