package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmScheduleAdjustment;

/**
 * 调停课申请Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmScheduleAdjustmentMapper 
{
    public TpmScheduleAdjustment selectTpmScheduleAdjustmentByAdjustId(Long adjustId);
    public List<TpmScheduleAdjustment> selectTpmScheduleAdjustmentList(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int insertTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int updateTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment);
    public int deleteTpmScheduleAdjustmentByAdjustId(Long adjustId);
    public int deleteTpmScheduleAdjustmentByAdjustIds(Long[] adjustIds);
}
