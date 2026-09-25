package com.yu.system.notify;

/**
 * 短信发送网关抽象（S6 多渠道通知）。
 * 默认提供 {@link LoggingSmsSender} 仅留痕不真实下发；
 * 对接学校短信平台时，实现本接口并标注 @Primary 即可无缝替换，无需改动调用方。
 */
public interface SmsSender
{
    /**
     * 发送短信。
     *
     * @param phone   手机号
     * @param content 短信正文
     * @return 是否发送成功
     * @throws Exception 网关异常
     */
    boolean send(String phone, String content) throws Exception;
}
