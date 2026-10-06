package com.yu.dis.quality;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.service.IDisSyncTaskService;

/**
 * 数据对接质量看板服务（N2 质量看板）。
 *
 * <p>基于既有同步任务 {@code dis_sync_task} 的执行量/失败量做只读聚合，输出成功率、
 * 逐任务质量与最高失败源。无需新增表。
 *
 * @author N2
 */
@Service
public class DisQualityService
{
    @Autowired
    private IDisSyncTaskService disSyncTaskService;

    /** 生成当前质量看板 */
    public DisQualityVo dashboard()
    {
        List<DisSyncTask> tasks = disSyncTaskService.selectDisSyncTaskList(new DisSyncTask());
        return compute(tasks, System.currentTimeMillis());
    }

    /**
     * 纯聚合计算（包内可见，便于以固定数据单测）。
     *
     * @param tasks     同步任务列表
     * @param nowMillis 时间基准
     */
    DisQualityVo compute(List<DisSyncTask> tasks, long nowMillis)
    {
        DisQualityVo vo = new DisQualityVo();
        vo.setGeneratedAt(new Date(nowMillis));
        if (tasks == null || tasks.isEmpty())
        {
            return vo;
        }

        long totalExecute = 0L;
        long totalFail = 0L;
        double worstFailRate = -1D;
        String worstTaskName = null;
        List<DisQualityVo.TaskQuality> details = new ArrayList<>();

        for (DisSyncTask t : tasks)
        {
            long exec = nz(t.getExecuteCount());
            long fail = nz(t.getFailCount());
            totalExecute += exec;
            totalFail += fail;

            DisQualityVo.TaskQuality q = new DisQualityVo.TaskQuality();
            q.setTaskId(t.getTaskId());
            q.setTaskName(t.getTaskName());
            q.setTaskCode(t.getTaskCode());
            q.setExecuteCount(exec);
            q.setFailCount(fail);
            double failRate = exec <= 0 ? 0D : round(fail * 100.0 / exec);
            q.setFailRate(failRate);
            q.setLastExecuteTime(t.getLastExecuteTime());
            details.add(q);

            if (failRate > worstFailRate)
            {
                worstFailRate = failRate;
                worstTaskName = t.getTaskName();
            }
        }

        vo.setTaskCount(tasks.size());
        vo.setTotalExecute(totalExecute);
        vo.setTotalFail(totalFail);
        vo.setSuccessRate(totalExecute <= 0 ? 100D : round((totalExecute - totalFail) * 100.0 / totalExecute));
        vo.setWorstTaskName(worstTaskName);
        vo.setWorstFailRate(Math.max(worstFailRate, 0D));
        vo.setTasks(details);
        return vo;
    }

    private static long nz(Integer v)
    {
        return v == null ? 0L : v;
    }

    private static double round(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }
}
