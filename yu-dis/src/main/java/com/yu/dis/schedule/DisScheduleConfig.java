package com.yu.dis.schedule;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用 Spring 定时任务调度，供 D1 同步任务的到期调度器使用。
 *
 * @author ruoyi
 */
@Configuration
@EnableScheduling
public class DisScheduleConfig
{
}
