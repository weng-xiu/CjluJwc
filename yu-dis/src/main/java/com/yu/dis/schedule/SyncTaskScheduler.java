package com.yu.dis.schedule;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.mapper.DisSyncTaskMapper;
import com.yu.dis.service.IDisSyncTaskService;

/**
 * 同步任务到期调度器（D1 链路调度端）。
 *
 * 周期性扫描"已启用且到达下次执行时间"的同步任务并触发执行；
 * 每次执行的完整链路（调用—解析—落库—留痕）与下次执行时间计算由
 * {@link IDisSyncTaskService#executeSyncTask(Long)} 负责。
 *
 * @author ruoyi
 */
@Component
public class SyncTaskScheduler
{
    private static final Logger log = LoggerFactory.getLogger(SyncTaskScheduler.class);

    @Autowired
    private DisSyncTaskMapper disSyncTaskMapper;

    @Autowired
    private IDisSyncTaskService disSyncTaskService;

    /** 每 60 秒扫描一次到期任务 */
    @Scheduled(fixedDelayString = "${dis.sync.scan-interval-ms:60000}", initialDelay = 30000)
    public void scanAndExecute()
    {
        List<DisSyncTask> dueTasks;
        try
        {
            dueTasks = disSyncTaskMapper.selectDueSyncTasks(new Date());
        }
        catch (Exception e)
        {
            log.error("扫描到期同步任务失败：{}", e.getMessage(), e);
            return;
        }
        if (dueTasks == null || dueTasks.isEmpty())
        {
            return;
        }
        for (DisSyncTask task : dueTasks)
        {
            try
            {
                log.info("调度器触发同步任务[{}]（cron={}）", task.getTaskName(), task.getCronExpression());
                disSyncTaskService.executeSyncTask(task.getTaskId());
            }
            catch (Exception e)
            {
                log.error("同步任务[{}]调度执行异常：{}", task.getTaskName(), e.getMessage(), e);
            }
        }
    }
}
