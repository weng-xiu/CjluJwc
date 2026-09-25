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

    /**
     * 审批通过
     *
     * @param adjustId       调停课申请ID
     * @param approveComment 审批意见
     */
    public void approve(Long adjustId, String approveComment);

    /**
     * 审批驳回
     *
     * @param adjustId       调停课申请ID
     * @param approveComment 审批意见
     */
    public void reject(Long adjustId, String approveComment);

    /**
     * P6：申请人撤销本人待审的调停课申请
     *
     * @param adjustId 调停课申请ID
     * @param operator 操作人用户名
     */
    public int cancelByApplicant(Long adjustId, String operator);
}
