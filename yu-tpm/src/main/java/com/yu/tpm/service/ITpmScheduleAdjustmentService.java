package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmScheduleAdjustment;

/**
 * 调停课申请Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmScheduleAdjustmentService 
{
    public TpmScheduleAdjustment selectTpmScheduleAdjustmentByAdjustId(Long adjustId);
    public List<TpmScheduleAdjustment> selectTpmScheduleAdjustmentList(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int insertTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int updateTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int deleteTpmScheduleAdjustmentByAdjustIds(Long[] adjustIds);
    public int deleteTpmScheduleAdjustmentByAdjustId(Long adjustId);
}
