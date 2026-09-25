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
import com.yu.common.core.domain.entity.SysUser;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 学籍异动Service实现（接入Flowable多级审批+回写联动）
 */
@Service
public class SamStatusChangeServiceImpl implements ISamStatusChangeService
{
    private static final Logger log = LoggerFactory.getLogger(SamStatusChangeServiceImpl.class);

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

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

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
            // P6：重复提交校验改走不受数据权限影响的查询
            SamStatusChange query = new SamStatusChange();
            query.setStudentId(samStatusChange.getStudentId());
            query.setApproveStatus("0");
            List<SamStatusChange> pendingList = samStatusChangeMapper.selectMyStatusChangeList(query);
            if (pendingList != null && !pendingList.isEmpty())
            {
                throw new ServiceException("该学生已有处理中的学籍异动申请，不允许重复提交");
            }
        }
        samStatusChange.setCreateTime(DateUtils.getNowDate());
        samStatusChange.setApproveStatus("0");
        // P6：创建人兜底为当前登录用户（门户审批结果消息与本人查询依赖 create_by）
        if (samStatusChange.getCreateBy() == null)
        {
            samStatusChange.setCreateBy(SecurityUtils.getUsername());
        }
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
        int rows = samStatusChangeMapper.updateSamStatusChange(update);
        // P1：审批自动产生待办——向院系审批人推送待办与消息
        notifyApprovalTodo(change, deptApprover);
        return rows;
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
            // P1：审批完成通知申请并办结相关待办
            notifyApplicant(change, "学籍异动申请已通过", "您的学籍异动申请（编号" + changeId + "）已审批通过，学籍状态已联动更新。");
            completeStatusChangeTodos(changeId);
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
        // P1：驳回通知申请并办结相关待办
        notifyApplicant(change, "学籍异动申请被驳回", "您的学籍异动申请（编号" + changeId + "）已被驳回。意见：" + (comment != null ? comment : "无"));
        completeStatusChangeTodos(changeId);
        return 1;
    }

    @Override
    public List<SamStatusChange> selectMyStatusChangeList(SamStatusChange samStatusChange)
    {
        return samStatusChangeMapper.selectMyStatusChangeList(samStatusChange);
    }

    @Override
    public boolean hasPendingChange(Long studentId)
    {
        SamStatusChange query = new SamStatusChange();
        query.setStudentId(studentId);
        query.setApproveStatus("0");
        List<SamStatusChange> pending = samStatusChangeMapper.selectMyStatusChangeList(query);
        return pending != null && !pending.isEmpty();
    }

    @Override
    @Transactional
    public int cancelByApplicant(Long changeId, String operator)
    {
        SamStatusChange change = samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
        if (change == null)
        {
            throw new ServiceException("异动记录不存在");
        }
        if (!"0".equals(change.getApproveStatus()))
        {
            throw new ServiceException("仅审批中的申请可撤销");
        }
        // 撤销Flowable流程实例（异常不阻断撤销落库）
        if (change.getProcInstId() != null)
        {
            try
            {
                oaWorkflowService.cancelProcessInstance(change.getProcInstId(), "申请人撤销");
            }
            catch (Exception e)
            {
                log.warn("撤销流程实例失败（changeId={}）：{}", changeId, e.getMessage());
            }
            updateProcessInstanceStatus(change.getProcInstId(), "3");
        }
        SamStatusChange update = new SamStatusChange();
        update.setChangeId(changeId);
        update.setApproveStatus("3"); // 0待审 1通过 2驳回 3已撤销
        update.setApproveBy(operator);
        update.setApproveTime(DateUtils.getNowDate());
        update.setApproveOpinion("申请人撤销");
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samStatusChangeMapper.updateSamStatusChange(update);
        completeStatusChangeTodos(changeId);
        return rows;
    }

    @Override
    public Map<String, Object> traceChange(Long changeId)
    {
        SamStatusChange change = samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId);
        if (change == null || change.getProcInstId() == null)
        {
            return null;
        }
        return oaWorkflowService.getProcessInstanceDetail(change.getProcInstId());
    }

    // ========== 私有方法 ==========

    /** 根据用户名解析用户ID（失败返回 null，不阻断业务） */
    private Long resolveUserId(String userName)
    {
        if (userName == null || userName.trim().isEmpty())
        {
            return null;
        }
        try
        {
            SysUser user = sysUserService.selectUserByUserName(userName);
            return user != null ? user.getUserId() : null;
        }
        catch (Exception e)
        {
            log.warn("解析用户[{}]失败：{}", userName, e.getMessage());
            return null;
        }
    }

    /** P1：向院系审批人推送待办+消息（异常不影响主流程） */
    private void notifyApprovalTodo(SamStatusChange change, String approver)
    {
        try
        {
            Long receiverId = resolveUserId(approver);
            if (receiverId == null)
            {
                return;
            }
            String title = "学籍异动待审批（编号" + change.getChangeId() + "）";
            SysTodo todo = new SysTodo();
            todo.setReceiverId(receiverId);
            todo.setTodoType("1");
            todo.setTitle(title);
            todo.setBusinessType("statusChange");
            todo.setBusinessId(change.getChangeId());
            todo.setCreateBy(SecurityUtils.getUsername());
            sysTodoService.createTodo(todo);

            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent("您有一条学籍异动申请待审批，学生ID=" + change.getStudentId() + "，异动类型=" + change.getChangeType());
            msg.setBusinessType("statusChange");
            msg.setBusinessId(change.getChangeId());
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("异动待办推送失败（changeId={}）", change.getChangeId(), e);
        }
    }

    /** P1：向申请发起人推送审批结果消息 */
    private void notifyApplicant(SamStatusChange change, String title, String content)
    {
        try
        {
            String applicant = change.getCreateBy();
            Long receiverId = resolveUserId(applicant);
            if (receiverId == null)
            {
                return;
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType("statusChange");
            msg.setBusinessId(change.getChangeId());
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("异动结果消息推送失败（changeId={}）", change.getChangeId(), e);
        }
    }

    /** P1：办结该异动相关的未办待办 */
    private void completeStatusChangeTodos(Long changeId)
    {
        try
        {
            SysTodo query = new SysTodo();
            query.setBusinessType("statusChange");
            query.setStatus("0");
            List<SysTodo> todos = sysTodoService.selectTodoList(query);
            if (todos != null)
            {
                for (SysTodo t : todos)
                {
                    if (changeId.equals(t.getBusinessId()))
                    {
                        sysTodoService.completeTodo(t.getTodoId(), t.getReceiverId());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("办结异动待办失败（changeId={}）", changeId, e);
        }
    }

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
