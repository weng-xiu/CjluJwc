package com.yu.tpm.service;

import com.yu.brm.domain.BrmClassroomBorrow;

/**
 * 教室借用审批流程Service（B1，Flowable 两级审批：院系 -> 教务处）
 *
 * 说明：编排逻辑放在 yu-tpm（可同时访问 yu-brm 借用数据与 yu-oa Flowable、yu-system 消息/待办），
 * 借用实体与冲突校验/占用日历仍归属 yu-brm，避免 yu-brm 反向依赖 yu-oa/yu-system 造成循环。
 *
 * @author yu
 * @date 2026-09-21
 */
public interface IBrmBorrowFlowService
{
    /** 提交借用申请：冲突校验通过后启动 Flowable 流程，置为待院系审核 */
    public BrmClassroomBorrow submit(Long borrowId);

    /** 院系初审：通过则流转至教务处终审，驳回则终止流程 */
    public void deptApprove(Long borrowId, boolean approved, String opinion);

    /** 教务处终审：通过则置为已通过并二次冲突校验，驳回则终止流程 */
    public void aaApprove(Long borrowId, boolean approved, String opinion);

    /** 撤销申请（运行中流程取消，置为已撤销） */
    public void cancel(Long borrowId);

    /** 获取流程实例详情（含历史任务链与审批意见），用于全流程可追溯 */
    public java.util.Map<String, Object> trace(Long borrowId);
}
