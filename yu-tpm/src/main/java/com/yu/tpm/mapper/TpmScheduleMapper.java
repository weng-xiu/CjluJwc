package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmSchedule;

/**
 * 排课Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmScheduleMapper 
{
    public TpmSchedule selectTpmScheduleByScheduleId(Long scheduleId);
    public List<TpmSchedule> selectTpmScheduleList(TpmSchedule tpmSchedule);
    public int insertTpmSchedule(TpmSchedule tpmSchedule);
    public int updateTpmSchedule(TpmSchedule tpmSchedule);
    public int deleteTpmScheduleByScheduleId(Long scheduleId);
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds);
}
