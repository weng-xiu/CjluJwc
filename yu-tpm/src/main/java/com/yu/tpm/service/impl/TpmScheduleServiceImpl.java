package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 排课Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmScheduleServiceImpl implements ITpmScheduleService 
{
    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Override
    public TpmSchedule selectTpmScheduleByScheduleId(Long scheduleId)
    {
        return tpmScheduleMapper.selectTpmScheduleByScheduleId(scheduleId);
    }

    @Override
    public List<TpmSchedule> selectTpmScheduleList(TpmSchedule tpmSchedule)
    {
        return tpmScheduleMapper.selectTpmScheduleList(tpmSchedule);
    }

    @Transactional
    @Override
    public int insertTpmSchedule(TpmSchedule tpmSchedule)
    {
        tpmSchedule.setCreateTime(DateUtils.getNowDate());
        return tpmScheduleMapper.insertTpmSchedule(tpmSchedule);
    }

    @Transactional
    @Override
    public int updateTpmSchedule(TpmSchedule tpmSchedule)
    {
        tpmSchedule.setUpdateTime(DateUtils.getNowDate());
        return tpmScheduleMapper.updateTpmSchedule(tpmSchedule);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleByScheduleId(Long scheduleId)
    {
        return tpmScheduleMapper.deleteTpmScheduleByScheduleId(scheduleId);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds)
    {
        return tpmScheduleMapper.deleteTpmScheduleByScheduleIds(scheduleIds);
    }
}
