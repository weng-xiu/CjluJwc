package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisDataExchangeLog;

/**
 * 数据交换日志Service接口
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public interface IDisDataExchangeLogService 
{
    public DisDataExchangeLog selectDisDataExchangeLogByLogId(Long logId);
    public List<DisDataExchangeLog> selectDisDataExchangeLogList(DisDataExchangeLog disDataExchangeLog);
    public int insertDisDataExchangeLog(DisDataExchangeLog disDataExchangeLog);
    public int deleteDisDataExchangeLogByLogId(Long logId);
    public int deleteDisDataExchangeLogByLogIds(Long[] logIds);
    public int cleanDisDataExchangeLog();
}
