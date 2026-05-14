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
}
