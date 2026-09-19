package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisSyncTask;

public interface DisSyncTaskMapper 
{
    public DisSyncTask selectDisSyncTaskByTaskId(Long taskId);
    public List<DisSyncTask> selectDisSyncTaskList(DisSyncTask disSyncTask);
    public int insertDisSyncTask(DisSyncTask disSyncTask);
    public int updateDisSyncTask(DisSyncTask disSyncTask);
    public int deleteDisSyncTaskByTaskId(Long taskId);
    public int deleteDisSyncTaskByTaskIds(Long[] taskIds);

    /** 查询已启用且到达下次执行时间的任务（next_execute_time 为空视为立即到期） */
    public List<DisSyncTask> selectDueSyncTasks(java.util.Date now);
}
