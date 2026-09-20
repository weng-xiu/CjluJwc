package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisSyncTask;

/**
 * 数据同步任务Service接口
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public interface IDisSyncTaskService 
{
    public DisSyncTask selectDisSyncTaskByTaskId(Long taskId);
    public List<DisSyncTask> selectDisSyncTaskList(DisSyncTask disSyncTask);
    public int insertDisSyncTask(DisSyncTask disSyncTask);
    public int updateDisSyncTask(DisSyncTask disSyncTask);
    public int deleteDisSyncTaskByTaskId(Long taskId);
    public int deleteDisSyncTaskByTaskIds(Long[] taskIds);

    /**
     * 执行同步任务
     * 调用外部系统接口进行数据交换，支持重试
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    public java.util.Map<String, Object> executeSyncTask(Long taskId);

    /**
     * 人工重推（D2）：手动触发一次补偿同步，交换日志标记重推标识
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    public java.util.Map<String, Object> rePushTask(Long taskId);
}
