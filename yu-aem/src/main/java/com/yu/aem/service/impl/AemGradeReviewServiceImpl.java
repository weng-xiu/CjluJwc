package com.yu.aem.service.impl;

import java.util.Date;
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
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.workflow.service.IOaWorkflowService;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IAemGradeReviewService;

/**
 * 成绩复核审批Service业务层处理
 * O1：成绩变更接入 Flowable 多级审批（课程负责人初审 → 教务处终审），
 * 状态机 0待审/初审中 4待教务处终审 1通过 2驳回 3已撤销；
 * 未进入流程的存量单仍走旧单级口径。
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemGradeReviewServiceImpl implements IAemGradeReviewService 
{
    private static final Logger log = LoggerFactory.getLogger(AemGradeReviewServiceImpl.class);

    /** 业务类型标识（贯穿 Flowable businessKey、待办、消息） */
    private static final String BUSINESS_TYPE = "gradeReview";
    private static final String PROCESS_KEY = "aem-grade-review-flow";

    // 审批状态机：0待审/初审中 4待教务处终审 1通过 2驳回 3已撤销
    private static final String ST_PENDING = "0";
    private static final String ST_AA = "4";
    private static final String ST_APPROVED = "1";
    private static final String ST_REJECTED = "2";
    private static final String ST_CANCELLED = "3";

    @Autowired
    private AemGradeReviewMapper aemGradeReviewMapper;

    @Autowired
    private AemGradeRecordMapper aemGradeRecordMapper;

    @Autowired
    private IAemGradeRecordService aemGradeRecordService;

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

    /** 课程负责人初审人兑底（可配置，默认 admin） */
    @Value("${aem.review.deptApprover:admin}")
    private String deptApproverConfig;

    /** 教务处终审人（可配置，默认 admin） */
    @Value("${aem.review.aaApprover:admin}")
    private String aaApproverConfig;

    @Override
    public AemGradeReview selectAemGradeReviewByReviewId(Long reviewId)
    {
        return aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
    }

    @Override
    public List<AemGradeReview> selectAemGradeReviewList(AemGradeReview aemGradeReview)
    {
        return aemGradeReviewMapper.selectAemGradeReviewList(aemGradeReview);
    }

    @Override
    @Transactional
    public int insertAemGradeReview(AemGradeReview aemGradeReview)
    {
        // 业务校验：复核原因不能为空
        if (StringUtils.isEmpty(aemGradeReview.getReviewReason()))
        {
            throw new ServiceException("复核原因不能为空");
        }
        // 同一成绩不允许存在多笔审批中的变更申请
        if (aemGradeReview.getGradeId() != null && hasPendingReview(aemGradeReview.getGradeId(), null))
        {
            throw new ServiceException("该成绩已存在审批中的变更申请，不允许重复提交");
        }
        aemGradeReview.setCreateTime(DateUtils.getNowDate());
        // 默认待审，创建人兑底当前登录用户（结果通知与撤销判断依赖 create_by）
        if (StringUtils.isEmpty(aemGradeReview.getApproveStatus()))
        {
            aemGradeReview.setApproveStatus(ST_PENDING);
        }
        if (aemGradeReview.getCreateBy() == null)
        {
            aemGradeReview.setCreateBy(SecurityUtils.getUsername());
        }
        return aemGradeReviewMapper.insertAemGradeReview(aemGradeReview);
    }

    @Override
    @Transactional
    public int updateAemGradeReview(AemGradeReview aemGradeReview)
    {
        // 状态校验：已办结或已进入流程的申请不允许直接修改
        AemGradeReview existing = aemGradeReviewMapper.selectAemGradeReviewByReviewId(aemGradeReview.getReviewId());
        if (existing != null && (ST_APPROVED.equals(existing.getApproveStatus())
                || ST_REJECTED.equals(existing.getApproveStatus())
                || ST_CANCELLED.equals(existing.getApproveStatus())))
        {
            throw new ServiceException("已办结的复核审批不允许修改");
        }
        if (existing != null && existing.getProcInstId() != null)
        {
            throw new ServiceException("该申请已进入审批流程，请通过审批/撤销操作处理");
        }
        aemGradeReview.setUpdateTime(DateUtils.getNowDate());
        return aemGradeReviewMapper.updateAemGradeReview(aemGradeReview);
    }

    @Override
    @Transactional
    public int deleteAemGradeReviewByReviewId(Long reviewId)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewId(reviewId);
    }

    @Override
    @Transactional
    public int deleteAemGradeReviewByReviewIds(Long[] reviewIds)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewIds(reviewIds);
    }

    /**
     * 审批成绩复核（旧单级口径，仅限未进入流程的存量单）
     * 通过后回写成绩并触发GPA重算
     */
    @Override
    @Transactional
    public int approveReview(Long reviewId, boolean approved, String approveBy, String approveOpinion)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null)
        {
            throw new ServiceException("复核记录不存在");
        }
        if (review.getProcInstId() != null || ST_AA.equals(review.getApproveStatus()))
        {
            throw new ServiceException("该申请已接入审批流程，请使用流程审批入口");
        }
        if (!ST_PENDING.equals(review.getApproveStatus()))
        {
            throw new ServiceException("该复核记录已审批，不可重复审批");
        }
        // 更新审批状态
        review.setApproveStatus(approved ? ST_APPROVED : ST_REJECTED);
        review.setApproveBy(approveBy);
        review.setApproveTime(new Date());
        review.setApproveOpinion(approveOpinion);
        review.setUpdateTime(DateUtils.getNowDate());
        int rows = aemGradeReviewMapper.updateAemGradeReview(review);
        // 如果通过且是成绩修改类型，回写成绩
        if (approved && "0".equals(review.getReviewType()) && review.getNewScore() != null)
        {
            writeBackGrade(review);
        }
        return rows;
    }

    /**
     * O1：提交复核申请并启动 Flowable 多级审批流程（课程负责人初审 → 教务处终审）
     */
    @Override
    @Transactional
    public int submitForApproval(Long reviewId)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null)
        {
            throw new ServiceException("复核记录不存在");
        }
        if (review.getProcInstId() != null)
        {
            throw new ServiceException("该申请已进入审批流程，请勿重复提交");
        }
        if (!ST_PENDING.equals(review.getApproveStatus()))
        {
            throw new ServiceException("仅待审状态的申请可提交进入流程");
        }
        if (hasPendingReview(review.getGradeId(), reviewId))
        {
            throw new ServiceException("该成绩已存在审批中的变更申请，不允许重复提交");
        }
        String starter = SecurityUtils.getUsername();
        String deptApprover = resolveDeptApprover(review, starter);
        String aaApprover = StringUtils.isNotEmpty(aaApproverConfig) ? aaApproverConfig : "admin";

        Map<String, Object> variables = new HashMap<>();
        variables.put("reviewId", reviewId);
        variables.put("gradeId", review.getGradeId());
        variables.put("studentId", review.getStudentId());
        variables.put("starter", starter);
        variables.put("deptApprover", deptApprover);
        variables.put("aaApprover", aaApprover);

        ProcessInstance processInstance = oaWorkflowService.startProcessInstance(
                PROCESS_KEY, BUSINESS_TYPE + ":" + reviewId, variables);

        OaProcessInstance instance = new OaProcessInstance();
        instance.setBusinessType(BUSINESS_TYPE);
        instance.setBusinessId(reviewId);
        instance.setProcInstId(processInstance.getId());
        instance.setStarterId(SecurityUtils.getLoginUser().getUserId());
        instance.setStarterName(starter);
        instance.setProcessStatus("0");
        instance.setStartTime(DateUtils.getNowDate());
        instance.setCreateTime(DateUtils.getNowDate());
        oaProcessInstanceMapper.insertOaProcessInstance(instance);

        AemGradeReview update = new AemGradeReview();
        update.setReviewId(reviewId);
        update.setProcInstId(processInstance.getId());
        update.setApproveStatus(ST_PENDING);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = aemGradeReviewMapper.updateAemGradeReview(update);

        notifyTodo(deptApprover, reviewId, "成绩变更待课程负责人初审（编号" + reviewId + "）",
                "您有一条成绩变更申请待初审，学生ID=" + review.getStudentId()
                        + "，原成绩=" + review.getOriginalScore() + " → 新成绩=" + review.getNewScore()
                        + "，原因：" + (review.getReviewReason() != null ? review.getReviewReason() : "无"));
        return rows;
    }

    /**
     * O1：流程审批，按当前状态路由：0=课程负责人初审，4=教务处终审
     */
    @Override
    @Transactional
    public int approveReviewByFlow(Long reviewId, boolean approved, String opinion)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null)
        {
            throw new ServiceException("复核记录不存在");
        }
        if (review.getProcInstId() == null)
        {
            // 未接入流程的存量单：回退旧单级口径
            return approveReview(reviewId, approved, SecurityUtils.getUsername(), opinion);
        }
        String stage = review.getApproveStatus();
        if (!ST_PENDING.equals(stage) && !ST_AA.equals(stage))
        {
            throw new ServiceException("该申请当前状态不可审批");
        }
        if (!approved && StringUtils.isEmpty(opinion))
        {
            throw new ServiceException("驳回必须填写审批意见");
        }
        completeActiveTask(review.getProcInstId(), approved, opinion);
        String assignee = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();

        AemGradeReview update = new AemGradeReview();
        update.setReviewId(reviewId);
        update.setApproveBy(assignee);
        update.setApproveTime(now);
        update.setUpdateTime(now);

        if (ST_PENDING.equals(stage))
        {
            // 初审阶段
            update.setDeptApproveBy(assignee);
            update.setDeptApproveTime(now);
            update.setDeptOpinion(opinion);
            if (approved)
            {
                update.setApproveStatus(ST_AA);
                aemGradeReviewMapper.updateAemGradeReview(update);
                notifyTodo(StringUtils.isNotEmpty(aaApproverConfig) ? aaApproverConfig : "admin",
                        reviewId, "成绩变更待教务处终审（编号" + reviewId + "）",
                        "成绩变更申请已通过课程负责人初审，待教务处终审。学生ID=" + review.getStudentId());
            }
            else
            {
                update.setApproveStatus(ST_REJECTED);
                aemGradeReviewMapper.updateAemGradeReview(update);
                updateProcessInstanceStatus(review.getProcInstId(), "2");
                notifyResult(review, "成绩变更申请被驳回",
                        "您的成绩变更申请（编号" + reviewId + "）课程负责人初审未通过。意见：" + opinion, false);
                completeTodos(reviewId);
            }
        }
        else
        {
            // 终审阶段
            update.setAaApproveBy(assignee);
            update.setAaApproveTime(now);
            update.setAaOpinion(opinion);
            if (approved)
            {
                update.setApproveStatus(ST_APPROVED);
                aemGradeReviewMapper.updateAemGradeReview(update);
                // 成绩修改类通过时回写新成绩并重算GPA
                if ("0".equals(review.getReviewType()) && review.getNewScore() != null)
                {
                    writeBackGrade(review);
                }
                updateProcessInstanceStatus(review.getProcInstId(), "1");
                notifyResult(review, "成绩变更申请已通过",
                        "您的成绩变更申请（编号" + reviewId + "）已审批通过"
                                + ("0".equals(review.getReviewType()) ? "，成绩已更新为 " + review.getNewScore() + " 分。" : "。"),
                        true);
                completeTodos(reviewId);
            }
            else
            {
                update.setApproveStatus(ST_REJECTED);
                aemGradeReviewMapper.updateAemGradeReview(update);
                updateProcessInstanceStatus(review.getProcInstId(), "2");
                notifyResult(review, "成绩变更申请被驳回",
                        "您的成绩变更申请（编号" + reviewId + "）教务处终审未通过。意见：" + opinion, false);
                completeTodos(reviewId);
            }
        }
        return 1;
    }

    /**
     * O1：申请人撤销审批中的复核申请
     */
    @Override
    @Transactional
    public int cancelByApplicant(Long reviewId, String operator)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null)
        {
            throw new ServiceException("复核记录不存在");
        }
        String stage = review.getApproveStatus();
        if (!ST_PENDING.equals(stage) && !ST_AA.equals(stage))
        {
            throw new ServiceException("仅审批中的申请可撤销");
        }
        if (!SecurityUtils.isAdmin() && !operator.equals(review.getCreateBy()))
        {
            throw new ServiceException("仅申请人可撤销该申请");
        }
        if (review.getProcInstId() != null)
        {
            try
            {
                oaWorkflowService.cancelProcessInstance(review.getProcInstId(), "申请人撤销");
            }
            catch (Exception e)
            {
                log.warn("撤销流程实例失败（reviewId={}）：{}", reviewId, e.getMessage());
            }
            updateProcessInstanceStatus(review.getProcInstId(), "3");
        }
        AemGradeReview update = new AemGradeReview();
        update.setReviewId(reviewId);
        update.setApproveStatus(ST_CANCELLED);
        update.setApproveBy(operator);
        update.setApproveTime(DateUtils.getNowDate());
        update.setApproveOpinion("申请人撤销");
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = aemGradeReviewMapper.updateAemGradeReview(update);
        completeTodos(reviewId);
        return rows;
    }

    @Override
    public Map<String, Object> traceReview(Long reviewId)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null || review.getProcInstId() == null)
        {
            return null;
        }
        return oaWorkflowService.getProcessInstanceDetail(review.getProcInstId());
    }

    // ========== 私有方法 ==========

    /** 该成绩是否存在审批中（0/4）的其他申请 */
    private boolean hasPendingReview(Long gradeId, Long excludeReviewId)
    {
        AemGradeReview query = new AemGradeReview();
        query.setGradeId(gradeId);
        List<AemGradeReview> list = aemGradeReviewMapper.selectAemGradeReviewList(query);
        for (AemGradeReview r : list)
        {
            if ((ST_PENDING.equals(r.getApproveStatus()) || ST_AA.equals(r.getApproveStatus()))
                    && (excludeReviewId == null || !excludeReviewId.equals(r.getReviewId())))
            {
                return true;
            }
        }
        return false;
    }

    /** 终审通过后回写成绩、置已复核并重算GPA（失败不影响审批落库） */
    private void writeBackGrade(AemGradeReview review)
    {
        AemGradeRecord gradeRecord = aemGradeRecordMapper.selectAemGradeRecordByGradeId(review.getGradeId());
        if (gradeRecord == null)
        {
            return;
        }
        gradeRecord.setTotalScore(review.getNewScore());
        gradeRecord.setIsReviewed("1");
        gradeRecord.setUpdateTime(DateUtils.getNowDate());
        aemGradeRecordMapper.updateAemGradeRecord(gradeRecord);
        try
        {
            aemGradeRecordService.calculateStudentGpa(review.getStudentId(), gradeRecord.getSemesterId(), null);
        }
        catch (Exception e)
        {
            log.warn("成绩变更后GPA重算失败（studentId={}）：{}", review.getStudentId(), e.getMessage());
        }
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

    /** 解析课程负责人初审人：课程最近开课教师登录名，无则取配置兑底 */
    private String resolveDeptApprover(AemGradeReview review, String starter)
    {
        try
        {
            if (review.getCourseId() != null)
            {
                AemGradeRecord grade = aemGradeRecordMapper.selectAemGradeRecordByGradeId(review.getGradeId());
                String login = aemGradeReviewMapper.selectCourseTeacherLogin(review.getCourseId(),
                        grade != null ? grade.getSemesterId() : null);
                if (StringUtils.isNotEmpty(login) && !login.equals(starter))
                {
                    return login;
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析课程负责人失败（reviewId={}）：{}", review.getReviewId(), e.getMessage());
        }
        return StringUtils.isNotEmpty(deptApproverConfig) ? deptApproverConfig : "admin";
    }

    private Long resolveUserId(String userName)
    {
        if (StringUtils.isEmpty(userName))
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
    private void notifyTodo(String approver, Long reviewId, String title, String content)
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
            todo.setBusinessId(reviewId);
            todo.setCreateBy(SecurityUtils.getUsername());
            sysTodoService.createTodo(todo);

            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(BUSINESS_TYPE);
            msg.setBusinessId(reviewId);
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("成绩变更待办推送失败（reviewId={}）：{}", reviewId, e.getMessage());
        }
    }

    /** 向申请人推送审批结果消息；通过时同步通知学生 */
    private void notifyResult(AemGradeReview review, String title, String content, boolean alsoNotifyStudent)
    {
        try
        {
            Long receiverId = resolveUserId(review.getCreateBy());
            if (receiverId != null)
            {
                SysMessage msg = new SysMessage();
                msg.setReceiverId(receiverId);
                msg.setMsgType("1");
                msg.setTitle(title);
                msg.setContent(content);
                msg.setBusinessType(BUSINESS_TYPE);
                msg.setBusinessId(review.getReviewId());
                msg.setCreateBy(SecurityUtils.getUsername());
                sysMessageService.sendMessage(msg);
            }
        }
        catch (Exception e)
        {
            log.error("成绩变更结果消息推送失败（reviewId={}）", review.getReviewId(), e);
        }
        if (alsoNotifyStudent && review.getStudentId() != null)
        {
            try
            {
                Long studentUserId = aemGradeReviewMapper.selectStudentUserId(review.getStudentId());
                if (studentUserId == null)
                {
                    return;
                }
                SysMessage msg = new SysMessage();
                msg.setReceiverId(studentUserId);
                msg.setMsgType("0");
                msg.setTitle("成绩变更通知");
                msg.setContent("您的课程（ID=" + review.getCourseId() + "）成绩已变更为 "
                        + review.getNewScore() + " 分（原 " + review.getOriginalScore() + " 分）。" + title + "。" )
                ;
                msg.setBusinessType(BUSINESS_TYPE);
                msg.setBusinessId(review.getReviewId());
                msg.setCreateBy(SecurityUtils.getUsername());
                sysMessageService.sendMessage(msg);
            }
            catch (Exception e)
            {
                log.error("成绩变更学生通知失败（reviewId={}）", review.getReviewId(), e);
            }
        }
    }

    /** 办结该申请相关的未办待办 */
    private void completeTodos(Long reviewId)
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
                    if (reviewId.equals(t.getBusinessId()))
                    {
                        sysTodoService.completeTodo(t.getTodoId(), t.getReceiverId());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("办结成绩变更待办失败（reviewId={}）", reviewId, e);
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
