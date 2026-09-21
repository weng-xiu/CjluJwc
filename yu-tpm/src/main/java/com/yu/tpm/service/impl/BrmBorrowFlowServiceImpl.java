package com.yu.tpm.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flowable.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;
import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.workflow.service.IOaWorkflowService;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysDeptService;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;
import com.yu.tpm.service.IBrmBorrowFlowService;

/**
 * 教室借用审批流程Service实现（B1）
 *
 * @author yu
 * @date 2026-09-21
 */
@Service
public class BrmBorrowFlowServiceImpl implements IBrmBorrowFlowService
{
    private static final Logger log = LoggerFactory.getLogger(BrmBorrowFlowServiceImpl.class);

    /** 业务类型标识（贯穿 Flowable businessKey、待办、消息） */
    private static final String BUSINESS_TYPE = "classroomBorrow";
    private static final String PROCESS_KEY = "brm-classroom-borrow-flow";

    // 审批状态机：0待院系审核 1待教务处审核 2已通过 3已驳回 4已撤销
    private static final String ST_DEPT = "0";
    private static final String ST_AA = "1";
    private static final String ST_APPROVED = "2";
    private static final String ST_REJECTED = "3";
    private static final String ST_CANCELLED = "4";

    @Autowired
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @Autowired
    private IOaWorkflowService oaWorkflowService;

    @Autowired
    private OaProcessInstanceMapper oaProcessInstanceMapper;

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysDeptService sysDeptService;

    /** 教务处终审人（可配置，默认 admin） */
    @Value("${brm.borrow.aaApprover:admin}")
    private String aaApproverConfig;

    @Override
    @Transactional
    public BrmClassroomBorrow submit(Long borrowId)
    {
        BrmClassroomBorrow borrow = brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId);
        if (borrow == null)
        {
            throw new ServiceException("借用申请不存在");
        }
        if (borrow.getApproveStatus() != null && !ST_DEPT.equals(borrow.getApproveStatus()))
        {
            throw new ServiceException("该申请已进入流程或已办结，不能重复提交");
        }
        // 冲突校验：与排课占用、其他在用借用单
        List<String> conflicts = brmClassroomBorrowService.checkConflict(
                borrow.getClassroomId(), borrow.getBorrowDate(),
                borrow.getStartTime(), borrow.getEndTime(), borrowId);
        if (!conflicts.isEmpty())
        {
            throw new ServiceException("借用冲突：" + String.join("；", conflicts));
        }

        String starter = SecurityUtils.getUsername();
        String deptApprover = resolveDeptApprover(starter);
        String aaApprover = (aaApproverConfig != null && !aaApproverConfig.trim().isEmpty()) ? aaApproverConfig : "admin";

        Map<String, Object> variables = new HashMap<>();
        variables.put("borrowId", borrowId);
        variables.put("starter", starter);
        variables.put("deptApprover", deptApprover);
        variables.put("aaApprover", aaApprover);

        ProcessInstance processInstance = oaWorkflowService.startProcessInstance(
                PROCESS_KEY, BUSINESS_TYPE + ":" + borrowId, variables);

        OaProcessInstance instance = new OaProcessInstance();
        instance.setBusinessType(BUSINESS_TYPE);
        instance.setBusinessId(borrowId);
        instance.setProcInstId(processInstance.getId());
        instance.setStarterId(SecurityUtils.getLoginUser().getUserId());
        instance.setStarterName(starter);
        instance.setProcessStatus("0");
        instance.setStartTime(DateUtils.getNowDate());
        instance.setCreateTime(DateUtils.getNowDate());
        oaProcessInstanceMapper.insertOaProcessInstance(instance);

        BrmClassroomBorrow update = new BrmClassroomBorrow();
        update.setBorrowId(borrowId);
        update.setProcInstId(processInstance.getId());
        update.setApproveStatus(ST_DEPT);
        brmClassroomBorrowService.updateBrmClassroomBorrow(update);

        notifyTodo(deptApprover, borrowId, "教室借用待院系审批（编号" + borrowId + "）",
                "您有一条教室借用申请待院系审批，借用日期=" + borrow.getBorrowDate()
                        + "，用途=" + (borrow.getPurpose() != null ? borrow.getPurpose() : "无"));
        borrow.setApproveStatus(ST_DEPT);
        return borrow;
    }

    @Override
    @Transactional
    public void deptApprove(Long borrowId, boolean approved, String opinion)
    {
        BrmClassroomBorrow borrow = requireRunning(borrowId, ST_DEPT);
        completeActiveTask(borrow.getProcInstId(), approved, opinion);

        BrmClassroomBorrow update = new BrmClassroomBorrow();
        update.setBorrowId(borrowId);
        update.setDeptApproveBy(SecurityUtils.getUsername());
        update.setDeptApproveTime(DateUtils.getNowDate());
        update.setDeptOpinion(opinion);
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(DateUtils.getNowDate());

        if (approved)
        {
            update.setApproveStatus(ST_AA);
            brmClassroomBorrowService.updateBrmClassroomBorrow(update);
            String aaApprover = (aaApproverConfig != null && !aaApproverConfig.trim().isEmpty()) ? aaApproverConfig : "admin";
            notifyTodo(aaApprover, borrowId, "教室借用待教务处审批（编号" + borrowId + "）",
                    "教室借用申请已通过院系初审，待教务处终审。");
        }
        else
        {
            update.setApproveStatus(ST_REJECTED);
            brmClassroomBorrowService.updateBrmClassroomBorrow(update);
            updateProcessInstanceStatus(borrow.getProcInstId(), "2");
            notifyApplicant(borrow, "教室借用申请被驳回",
                    "您的教室借用申请（编号" + borrowId + "）院系初审未通过。意见：" + (opinion != null ? opinion : "无"));
            completeTodos(borrowId);
        }
    }

    @Override
    @Transactional
    public void aaApprove(Long borrowId, boolean approved, String opinion)
    {
        BrmClassroomBorrow borrow = requireRunning(borrowId, ST_AA);

        if (approved)
        {
            // 终审通过前二次冲突校验，保障"冲突时段不可借"
            List<String> conflicts = brmClassroomBorrowService.checkConflict(
                    borrow.getClassroomId(), borrow.getBorrowDate(),
                    borrow.getStartTime(), borrow.getEndTime(), borrowId);
            if (!conflicts.isEmpty())
            {
                throw new ServiceException("终审冲突，无法通过：" + String.join("；", conflicts));
            }
        }

        completeActiveTask(borrow.getProcInstId(), approved, opinion);

        BrmClassroomBorrow update = new BrmClassroomBorrow();
        update.setBorrowId(borrowId);
        update.setAaApproveBy(SecurityUtils.getUsername());
        update.setAaApproveTime(DateUtils.getNowDate());
        update.setAaOpinion(opinion);
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(DateUtils.getNowDate());

        if (approved)
        {
            update.setApproveStatus(ST_APPROVED);
            brmClassroomBorrowService.updateBrmClassroomBorrow(update);
            updateProcessInstanceStatus(borrow.getProcInstId(), "1");
            notifyApplicant(borrow, "教室借用申请已通过",
                    "您的教室借用申请（编号" + borrowId + "）已审批通过，可在借用时段使用该教室。");
            completeTodos(borrowId);
        }
        else
        {
            update.setApproveStatus(ST_REJECTED);
            brmClassroomBorrowService.updateBrmClassroomBorrow(update);
            updateProcessInstanceStatus(borrow.getProcInstId(), "2");
            notifyApplicant(borrow, "教室借用申请被驳回",
                    "您的教室借用申请（编号" + borrowId + "）教务处终审未通过。意见：" + (opinion != null ? opinion : "无"));
            completeTodos(borrowId);
        }
    }

    @Override
    @Transactional
    public void cancel(Long borrowId)
    {
        BrmClassroomBorrow borrow = brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId);
        if (borrow == null)
        {
            throw new ServiceException("借用申请不存在");
        }
        String st = borrow.getApproveStatus();
        if (!ST_DEPT.equals(st) && !ST_AA.equals(st))
        {
            throw new ServiceException("仅审批中的申请可撤销");
        }
        if (borrow.getProcInstId() != null)
        {
            try
            {
                oaWorkflowService.cancelProcessInstance(borrow.getProcInstId(), "申请人撤销");
            }
            catch (Exception e)
            {
                log.warn("撤销流程实例失败（borrowId={}）：{}", borrowId, e.getMessage());
            }
            updateProcessInstanceStatus(borrow.getProcInstId(), "3");
        }
        BrmClassroomBorrow update = new BrmClassroomBorrow();
        update.setBorrowId(borrowId);
        update.setApproveStatus(ST_CANCELLED);
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(DateUtils.getNowDate());
        brmClassroomBorrowService.updateBrmClassroomBorrow(update);
        completeTodos(borrowId);
    }

    @Override
    public Map<String, Object> trace(Long borrowId)
    {
        BrmClassroomBorrow borrow = brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId);
        if (borrow == null || borrow.getProcInstId() == null)
        {
            return null;
        }
        return oaWorkflowService.getProcessInstanceDetail(borrow.getProcInstId());
    }

    // ========== 私有辅助方法 ==========

    /** 校验借用单处于指定审批阶段且流程在运行 */
    private BrmClassroomBorrow requireRunning(Long borrowId, String expectStage)
    {
        BrmClassroomBorrow borrow = brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(borrowId);
        if (borrow == null)
        {
            throw new ServiceException("借用申请不存在");
        }
        if (borrow.getProcInstId() == null)
        {
            throw new ServiceException("该申请尚未提交进入审批流程");
        }
        if (!expectStage.equals(borrow.getApproveStatus()))
        {
            throw new ServiceException("当前审批阶段不匹配，无法处理（期望状态码 " + expectStage + "）");
        }
        return borrow;
    }

    /** 定位流程实例当前活动任务并完成（以任务实际办理人身份完成，兼容管理员代办） */
    @SuppressWarnings("unchecked")
    private void completeActiveTask(String procInstId, boolean approved, String comment)
    {
        Map<String, Object> detail = oaWorkflowService.getProcessInstanceDetail(procInstId);
        if (detail == null)
        {
            throw new ServiceException("流程实例不存在或已结束");
        }
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) detail.get("tasks");
        Map<String, Object> active = null;
        if (tasks != null)
        {
            for (Map<String, Object> t : tasks)
            {
                if (t.get("endTime") == null)
                {
                    active = t;
                    break;
                }
            }
        }
        if (active == null)
        {
            throw new ServiceException("没有待处理的审批任务");
        }
        String taskId = String.valueOf(active.get("taskId"));
        String assignee = active.get("assignee") != null ? String.valueOf(active.get("assignee")) : SecurityUtils.getUsername();
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        oaWorkflowService.completeTask(taskId, assignee, variables, comment);
    }

    /** 解析院系审批人：申请人所属部门负责人，回退 admin */
    private String resolveDeptApprover(String starter)
    {
        try
        {
            SysUser user = sysUserService.selectUserByUserName(starter);
            if (user != null && user.getDeptId() != null)
            {
                SysDept dept = sysDeptService.selectDeptById(user.getDeptId());
                if (dept != null && dept.getLeader() != null && !dept.getLeader().trim().isEmpty()
                        && !dept.getLeader().equals(starter))
                {
                    return dept.getLeader();
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析院系审批人失败（starter={}）：{}", starter, e.getMessage());
        }
        return "admin";
    }

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

    /** 向审批人推送待办+消息（异常不阻断主流程） */
    private void notifyTodo(String approver, Long borrowId, String title, String content)
    {
        try
        {
            Long receiverId = resolveUserId(approver);
            if (receiverId == null)
            {
                return;
            }
            SysTodo todo = new SysTodo();
            todo.setReceiverId(receiverId);
            todo.setTodoType("1");
            todo.setTitle(title);
            todo.setBusinessType(BUSINESS_TYPE);
            todo.setBusinessId(borrowId);
            todo.setCreateBy(SecurityUtils.getUsername());
            sysTodoService.createTodo(todo);

            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(BUSINESS_TYPE);
            msg.setBusinessId(borrowId);
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("教室借用待办推送失败：{}", e.getMessage());
        }
    }

    /** 向申请人推送审批结果消息 */
    private void notifyApplicant(BrmClassroomBorrow borrow, String title, String content)
    {
        try
        {
            String applicant = borrow.getApplicant() != null ? borrow.getApplicant() : borrow.getCreateBy();
            Long receiverId = borrow.getApplicantUserId() != null ? borrow.getApplicantUserId() : resolveUserId(applicant);
            if (receiverId == null)
            {
                return;
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(BUSINESS_TYPE);
            msg.setBusinessId(borrow.getBorrowId());
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("教室借用结果消息推送失败（borrowId={}）", borrow.getBorrowId(), e);
        }
    }

    /** 办结该借用申请相关的未办待办 */
    private void completeTodos(Long borrowId)
    {
        try
        {
            SysTodo query = new SysTodo();
            query.setBusinessType(BUSINESS_TYPE);
            query.setStatus("0");
            List<SysTodo> todos = sysTodoService.selectTodoList(query);
            if (todos != null)
            {
                for (SysTodo t : todos)
                {
                    if (borrowId.equals(t.getBusinessId())
                            || (t.getTitle() != null && t.getTitle().contains("编号" + borrowId)))
                    {
                        sysTodoService.completeTodo(t.getTodoId(), t.getReceiverId());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("办结教室借用待办失败（borrowId={}）", borrowId, e);
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
