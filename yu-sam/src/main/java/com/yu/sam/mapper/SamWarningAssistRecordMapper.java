package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamWarningAssistRecord;

/**
 * 学业预警帮扶跟踪记录 Mapper（S6）
 */
public interface SamWarningAssistRecordMapper
{
    public int insertSamWarningAssistRecord(SamWarningAssistRecord record);

    public List<SamWarningAssistRecord> selectByAssistId(@Param("assistId") Long assistId);

    public int deleteByAssistId(@Param("assistId") Long assistId);

    public int deleteByAssistIds(@Param("assistIds") Long[] assistIds);
}
