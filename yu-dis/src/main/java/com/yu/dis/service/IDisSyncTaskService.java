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
}
