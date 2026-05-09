package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmSchedule;

/**
 * 排课Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmScheduleService 
{
    public TpmSchedule selectTpmScheduleByScheduleId(Long scheduleId);
    public List<TpmSchedule> selectTpmScheduleList(TpmSchedule tpmSchedule);
    public int insertTpmSchedule(TpmSchedule tpmSchedule);
    public int updateTpmSchedule(TpmSchedule tpmSchedule);
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds);
    public int deleteTpmScheduleByScheduleId(Long scheduleId);
}
