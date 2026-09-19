package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamStatusChange;

public interface ISamStatusChangeService 
{
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId);
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange);
    public int insertSamStatusChange(SamStatusChange samStatusChange);
    public int updateSamStatusChange(SamStatusChange samStatusChange);
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds);
    public int deleteSamStatusChangeByChangeId(Long changeId);

    /**
     * 提交异动申请并启动Flowable多级审批流程
     */
    public int submitForApproval(Long changeId);

    /**
     * 审批通过（回写学籍状态+联动）
     */
    public int approveChange(Long changeId, String taskId, String comment);

    /**
     * 驳回异动申请
     */
    public int rejectChange(Long changeId, String taskId, String comment);
}
