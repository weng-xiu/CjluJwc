package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisDataExchangeLog;

public interface DisDataExchangeLogMapper 
{
    public DisDataExchangeLog selectDisDataExchangeLogByLogId(Long logId);
    public List<DisDataExchangeLog> selectDisDataExchangeLogList(DisDataExchangeLog disDataExchangeLog);
    public int insertDisDataExchangeLog(DisDataExchangeLog disDataExchangeLog);
    public int deleteDisDataExchangeLogByLogId(Long logId);
    public int deleteDisDataExchangeLogByLogIds(Long[] logIds);
    public int cleanDisDataExchangeLog();
}
