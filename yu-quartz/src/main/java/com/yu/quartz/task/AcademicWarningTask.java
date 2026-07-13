package com.yu.quartz.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yu.sam.service.impl.AcademicWarningEngine;

/**
 * 学业预警定时任务
 * 由Quartz调度，通过若依定时任务管理页面配置（sys_job表）
 * 调用目标：academicWarningTask.generateWarnings(1L)
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Component("academicWarningTask")
public class AcademicWarningTask
{
    private static final Logger log = LoggerFactory.getLogger(AcademicWarningTask.class);

    @Autowired
    private AcademicWarningEngine academicWarningEngine;

    /**
     * 批量生成学业预警（由Quartz调度，建议每日凌晨2:00执行）
     * 可通过若依定时任务管理页面配置
     * 
     * @param semesterId 学期ID
     */
    public void generateWarnings(Long semesterId)
    {
        log.info("学业预警定时任务开始执行，学期ID：{}", semesterId);
        try
        {
            academicWarningEngine.generateWarningsBatch(semesterId);
            log.info("学业预警定时任务执行完成");
        }
        catch (Exception e)
        {
            log.error("学业预警定时任务执行失败：{}", e.getMessage(), e);
        }
    }
}
