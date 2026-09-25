package com.yu.sam.service;

import java.util.List;
import java.util.Map;
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

    /**
     * P6：门户端本人异动记录查询（不走数据权限过滤）
     */
    public List<SamStatusChange> selectMyStatusChangeList(SamStatusChange samStatusChange);

    /**
     * P6：该学生是否已有审批中的异动申请（不受数据权限影响）
     */
    public boolean hasPendingChange(Long studentId);

    /**
     * P6：申请人撤销尚在审批中的异动申请（含流程实例撤销）
     */
    public int cancelByApplicant(Long changeId, String operator);

    /**
     * P6：审批进度追溯（Flowable 历史任务链，未进入流程返回 null）
     */
    public Map<String, Object> traceChange(Long changeId);
}
