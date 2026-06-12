package com.yu.dis.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.dis.mapper.DisDataExchangeLogMapper;
import com.yu.dis.domain.DisDataExchangeLog;
import com.yu.dis.service.IDisDataExchangeLogService;

/**
 * 数据交换日志Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
@Service
public class DisDataExchangeLogServiceImpl implements IDisDataExchangeLogService 
{
    @Autowired
    private DisDataExchangeLogMapper disDataExchangeLogMapper;

    @Override
    public DisDataExchangeLog selectDisDataExchangeLogByLogId(Long logId)
    {
        return disDataExchangeLogMapper.selectDisDataExchangeLogByLogId(logId);
    }

    @Override
    public List<DisDataExchangeLog> selectDisDataExchangeLogList(DisDataExchangeLog disDataExchangeLog)
    {
        return disDataExchangeLogMapper.selectDisDataExchangeLogList(disDataExchangeLog);
    }

    @Transactional
    @Override
    public int insertDisDataExchangeLog(DisDataExchangeLog disDataExchangeLog)
    {
        return disDataExchangeLogMapper.insertDisDataExchangeLog(disDataExchangeLog);
    }

    @Transactional
    @Override
    public int deleteDisDataExchangeLogByLogId(Long logId)
    {
        return disDataExchangeLogMapper.deleteDisDataExchangeLogByLogId(logId);
    }

    @Transactional
    @Override
    public int deleteDisDataExchangeLogByLogIds(Long[] logIds)
    {
        return disDataExchangeLogMapper.deleteDisDataExchangeLogByLogIds(logIds);
    }

    @Transactional
    @Override
    public int cleanDisDataExchangeLog()
    {
        return disDataExchangeLogMapper.cleanDisDataExchangeLog();
    }
}
