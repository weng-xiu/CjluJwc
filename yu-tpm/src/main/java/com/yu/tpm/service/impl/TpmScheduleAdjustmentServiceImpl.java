package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmScheduleAdjustmentMapper;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;

/**
 * 调停课申请Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmScheduleAdjustmentServiceImpl implements ITpmScheduleAdjustmentService 
{
    @Autowired
    private TpmScheduleAdjustmentMapper tpmScheduleAdjustmentMapper;

    @Override
    public TpmScheduleAdjustment selectTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Override
    public List<TpmScheduleAdjustment> selectTpmScheduleAdjustmentList(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int insertTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setCreateTime(DateUtils.getNowDate());
        return tpmScheduleAdjustmentMapper.insertTpmScheduleAdjustment(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int updateTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setUpdateTime(DateUtils.getNowDate());
        return tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustIds(Long[] adjustIds)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustIds(adjustIds);
    }
}
