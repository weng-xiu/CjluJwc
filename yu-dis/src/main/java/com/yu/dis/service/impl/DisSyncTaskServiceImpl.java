package com.yu.dis.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.dis.mapper.DisSyncTaskMapper;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.service.IDisSyncTaskService;

/**
 * 数据同步任务Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
@Service
public class DisSyncTaskServiceImpl implements IDisSyncTaskService 
{
    @Autowired
    private DisSyncTaskMapper disSyncTaskMapper;

    @Override
    public DisSyncTask selectDisSyncTaskByTaskId(Long taskId)
    {
        return disSyncTaskMapper.selectDisSyncTaskByTaskId(taskId);
    }

    @Override
    public List<DisSyncTask> selectDisSyncTaskList(DisSyncTask disSyncTask)
    {
        return disSyncTaskMapper.selectDisSyncTaskList(disSyncTask);
    }

    @Override
    public int insertDisSyncTask(DisSyncTask disSyncTask)
    {
        disSyncTask.setCreateTime(DateUtils.getNowDate());
        return disSyncTaskMapper.insertDisSyncTask(disSyncTask);
    }

    @Override
    public int updateDisSyncTask(DisSyncTask disSyncTask)
    {
        disSyncTask.setUpdateTime(DateUtils.getNowDate());
        return disSyncTaskMapper.updateDisSyncTask(disSyncTask);
    }

    @Override
    public int deleteDisSyncTaskByTaskId(Long taskId)
    {
        return disSyncTaskMapper.deleteDisSyncTaskByTaskId(taskId);
    }

    @Override
    public int deleteDisSyncTaskByTaskIds(Long[] taskIds)
    {
        return disSyncTaskMapper.deleteDisSyncTaskByTaskIds(taskIds);
    }
}
