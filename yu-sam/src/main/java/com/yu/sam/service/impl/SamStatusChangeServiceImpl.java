package com.yu.sam.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.workflow.service.IOaWorkflowService;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamLinkageMapper;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.service.ISamStatusChangeService;
import com.yu.sam.workflow.StatusChangeApprovalHandler;

/**
 * 学籍异动Service实现（接入Flowable多级审批+回写联动）
 */
@Service
public class SamStatusChangeServiceImpl implements ISamStatusChangeService
{
    @Autowired
    private SamStatusChangeMapper samStatusChangeMapper;

    @Autowired
    private SamStudentMapper samStudentMapper;

    @Autowired
    private SamLinkageMapper samLinkageMapper;

    @Autowired
    private StatusChangeApprovalHandler approvalHandler;

    @Autowired
    private IOaWorkflowService oaWorkflowService;

    @Autowired
    private OaProcessInstanceMapper oaProcessInstanceMapper;

    @Autowired
    private com.yu.system.service.ISysDeptService sysDeptService;

    @Override
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId)
    {
        return samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
    }

    @Override
    @com.yu.common.annotation.DataScope(deptAlias = "d", userAlias = "s")
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange)
    {
        return samStatusChangeMapper.selectSamStatusChangeList(samStatusChange);
    }

    @Override
    @Transactional
    public int insertSamStatusChange(SamStatusChange samStatusChange)
    {
        if (samStatusChange.getStudentId() != null)
        {
            SamStatusChange query = new SamStatusChange();
            query.setStudentId(samStatusChange.getStudentId());
            query.setApproveStatus("0");
            List<SamStatusChange> pendingList = samStatusChangeMapper.selectSamStatusChangeList(query);
            if (pendingList != null && !pendingList.isEmpty())
            {
                throw new ServiceException("该学生已有处理中的学籍异动申请，不允许重复提交");
            }
        }
        samStatusChange.setCreateTime(DateUtils.getNowDate());
        samStatusChange.setApproveStatus("0");
        return samStatusChangeMapper.insertSamStatusChange(samStatusChange);
    }

    @Override
    @Transactional
    public int updateSamStatusChange(SamStatusChange samStatusChange)
    {
        SamStatusChange existing = samStatusChangeMapper.selectSamStatusChangeByChangeId(samStatusChange.getChangeId());
        if (existing != null && "1".equals(existing.getApproveStatus()))
        {
            throw new ServiceException("已审批的学籍异动不允许修改");
        }
        samStatusChange.setUpdateTime(DateUtils.getNowDate());
        return samStatusChangeMapper.updateSamStatusChange(samStatusChange);
    }

    @Override
    @Transactional
    public int deleteSamStatusChangeByChangeId(Long changeId)
    {
        return samStatusChangeMapper.deleteSamStatusChangeByChangeId(changeId);
    }

    @Override
    @Transactional
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds)
    {
        return samStatusChangeMapper.deleteSamStatusChangeByChangeIds(changeIds);
    }

    @Override
    @Transactional
    public int submitForApproval(Long changeId)
    {
        SamStatusChange change = samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
        if (change == null)
        {
            throw new ServiceException("异动记录不存在");
        }
        String error = approvalHandler.validate(change);
        if (error != null)
        {
            throw new ServiceException(error);
        }
        String starter = SecurityUtils.getUsername();

        // 确定院系审批人和教务处审批人
        SamStudent student = samStudentMapper.selectSamStudentByStudentId(change.getStudentId());
        String deptApprover = resolveDeptApprover(student, starter);
        String academicAffairsApprover = resolveAcademicAffairsApprover(starter);

        Map<String, Object> variables = new HashMap<>();
        variables.put("changeId", changeId);
        variables.put("studentId", change.getStudentId());
        variables.put("changeType", change.getChangeType());
        variables.put("starter", starter);
        variables.put("deptApprover", deptApprover);
        variables.put("aaApprover", academicAffairsApprover);

        // 启动Flowable流程 (多级审批: 院系 -> 教务处)
        ProcessInstance processInstance = oaWorkflowService.startProcessInstance(
                "sam-status-change-flow", "statusChange:" + changeId, variables);

        // 记录流程实例关联
        OaProcessInstance instance = new OaProcessInstance();
        instance.setBusinessType("statusChange");
        instance.setBusinessId(changeId);
        instance.setProcInstId(processInstance.getId());
        instance.setStarterId(SecurityUtils.getLoginUser().getUserId());
        instance.setStarterName(starter);
        instance.setProcessStatus("0");
        instance.setStartTime(DateUtils.getNowDate());
        instance.setCreateTime(DateUtils.getNowDate());
        oaProcessInstanceMapper.insertOaProcessInstance(instance);

        // 更新异动记录
        SamStatusChange update = new SamStatusChange();
        update.setChangeId(changeId);
        update.setProcInstId(processInstance.getId());
        update.setApproveStatus("0"); // 仍在待审（但已进入流程）
        update.setUpdateTime(DateUtils.getNowDate());
        return samStatusChangeMapper.updateSamStatusChange(update);
    }

    @Override
    @Transactional
    public int approveChange(Long changeId, String taskId, String comment)
    {
        String assignee = SecurityUtils.getUsername();
        SamStatusChange change = samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
        if (change == null)
        {
            throw new ServiceException("异动记录不存在");
        }

        // 完成Flowable任务
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", true);
        oaWorkflowService.completeTask(taskId, assignee, variables, comment);

        // 检查流程是否已全部通过（无更多待办）
        boolean processFinished = isProcessFinished(change.getProcInstId(), assignee);

        if (processFinished)
        {
            // 回写学籍状态
            String targetStatus = approvalHandler.resolveTargetStatus(change.getChangeType(), change.getNewStatus());
            if (targetStatus != null)
            {
                SamStudent studentUpdate = new SamStudent();
                studentUpdate.setStudentId(change.getStudentId());
                studentUpdate.setStudentStatus(targetStatus);
                studentUpdate.setUpdateTime(DateUtils.getNowDate());
                samStudentMapper.updateSamStudent(studentUpdate);
            }

            // 执行联动操作
            SamStudent student = samStudentMapper.selectSamStudentByStudentId(change.getStudentId());
            executeLinkage(student, targetStatus);

            // 更新异动记录为通过
            SamStatusChange update = new SamStatusChange();
            update.setChangeId(changeId);
            update.setApproveStatus("1");
            update.setApproveBy(assignee);
            update.setApproveTime(DateUtils.getNowDate());
            update.setApproveOpinion(comment);
            update.setNewStatus(targetStatus);
            update.setUpdateTime(DateUtils.getNowDate());
            samStatusChangeMapper.updateSamStatusChange(update);

            // 更新流程实例状态
            updateProcessInstanceStatus(change.getProcInstId(), "1");
        }
        return 1;
    }

    @Override
    @Transactional
    public int rejectChange(Long changeId, String taskId, String comment)
    {
        String assignee = SecurityUtils.getUsername();
        SamStatusChange change = samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
        if (change == null)
        {
            throw new ServiceException("异动记录不存在");
        }
        oaWorkflowService.rejectTask(taskId, assignee, comment);

        // 更新异动记录为驳回
        SamStatusChange update = new SamStatusChange();
        update.setChangeId(changeId);
        update.setApproveStatus("2");
        update.setApproveBy(assignee);
        update.setApproveTime(DateUtils.getNowDate());
        update.setApproveOpinion(comment);
        update.setUpdateTime(DateUtils.getNowDate());
        samStatusChangeMapper.updateSamStatusChange(update);

        // 更新流程实例状态
        updateProcessInstanceStatus(change.getProcInstId(), "2");
        return 1;
    }

    // ========== 私有方法 ==========

    private String resolveDeptApprover(SamStudent student, String starter)
    {
        if (student != null && student.getDeptId() != null)
        {
            com.yu.common.core.domain.entity.SysDept dept = sysDeptService.selectDeptById(student.getDeptId());
            if (dept != null && dept.getLeader() != null && !dept.getLeader().trim().isEmpty()
                    && !dept.getLeader().equals(starter))
            {
                return dept.getLeader();
            }
        }
        // fallback：使用admin
        return "admin";
    }

    private String resolveAcademicAffairsApprover(String starter)
    {
        // 教务处审批人固定为admin，实际可配置
        return "admin";
    }

    private boolean isProcessFinished(String procInstId, String assignee)
    {
        if (procInstId == null)
        {
            return true;
        }
        // 检查该流程实例是否还有当前用户的待办
        org.flowable.task.api.Task remaining = oaWorkflowService.listTodoTasks(assignee, null).stream()
                .filter(t -> procInstId.equals(t.getProcessInstanceId()))
                .findFirst().orElse(null);
        // 也要检查其他审批人是否有待办（流程是否真正结束）
        // 简化：如果assignee完成且是最后一级，查流程实例状态
        List<Map<String, Object>> instances = oaWorkflowService.listProcessInstances(null, null, "running");
        for (Map<String, Object> inst : instances)
        {
            if (procInstId.equals(inst.get("processInstanceId")))
            {
                // 还有更多任务未完成（检查是否有其他人有待办）
                return checkNoMoreTasks(procInstId);
            }
        }
        return true; // 流程已不在运行中
    }

    private boolean checkNoMoreTasks(String procInstId)
    {
        // 查询流程实例详情看是否还有活跃任务
        Map<String, Object> detail = oaWorkflowService.getProcessInstanceDetail(procInstId);
        if (detail == null)
        {
            return true;
        }
        // 如果有endTime说明已结束
        return detail.get("endTime") != null;
    }

    private void executeLinkage(SamStudent student, String targetStatus)
    {
        if (student == null || targetStatus == null)
        {
            return;
        }
        List<String> actions = approvalHandler.buildLinkageActions(student, targetStatus);
        for (String action : actions)
        {
            if (action.startsWith("FREEZE_ENROLLMENT:"))
            {
                samLinkageMapper.freezeEnrollment(student.getStudentId());
            }
            else if (action.startsWith("UNFREEZE_ENROLLMENT:"))
            {
                samLinkageMapper.unfreezeEnrollment(student.getStudentId());
            }
            else if (action.startsWith("FLAG_GRADE:"))
            {
                samLinkageMapper.flagGradeRecords(student.getStudentId(), "学籍异动标记-" + targetStatus);
            }
            else if (action.startsWith("STOP_PAYMENT:"))
            {
                samLinkageMapper.stopPayment(student.getStudentId());
            }
            else if (action.startsWith("RESUME_PAYMENT:"))
            {
                samLinkageMapper.resumePayment(student.getStudentId());
            }
        }
    }

    private void updateProcessInstanceStatus(String procInstId, String status)
    {
        if (procInstId == null)
        {
            return;
        }
        OaProcessInstance query = new OaProcessInstance();
        query.setProcInstId(procInstId);
        List<OaProcessInstance> list = oaProcessInstanceMapper.selectOaProcessInstanceList(query);
        if (!list.isEmpty())
        {
            OaProcessInstance instance = list.get(0);
            instance.setProcessStatus(status);
            instance.setEndTime(DateUtils.getNowDate());
            instance.setUpdateTime(DateUtils.getNowDate());
            oaProcessInstanceMapper.updateOaProcessInstance(instance);
        }
    }
}
