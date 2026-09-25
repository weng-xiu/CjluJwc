package com.yu.tpm.service.impl;

import java.util.Date;
import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.schedule.TimeSlotUtils;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.mapper.TpmScheduleAdjustmentMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;

/**
 * 调停课申请Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmScheduleAdjustmentServiceImpl implements ITpmScheduleAdjustmentService 
{
    private static final Logger log = LoggerFactory.getLogger(TpmScheduleAdjustmentServiceImpl.class);

    @Autowired
    private TpmScheduleAdjustmentMapper tpmScheduleAdjustmentMapper;

    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ISysUserService sysUserService;

    /** 调停课审批人（可配置，默认 admin） */
    @Value("${tpm.adjust.approver:admin}")
    private String adjustApprover;

    @Override
    public TpmScheduleAdjustment selectTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Override
    public List<TpmScheduleAdjustment> selectTpmScheduleAdjustmentList(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int insertTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setCreateTime(DateUtils.getNowDate());
        // 新增时默认待审
        if (tpmScheduleAdjustment.getApproveStatus() == null
                || tpmScheduleAdjustment.getApproveStatus().trim().isEmpty())
        {
            tpmScheduleAdjustment.setApproveStatus("0");
        }
        int rows = tpmScheduleAdjustmentMapper.insertTpmScheduleAdjustment(tpmScheduleAdjustment);
        // P6：新申请待审——向审批人推送待办与消息（异常不阻断主流程）
        if ("0".equals(tpmScheduleAdjustment.getApproveStatus()))
        {
            notifyApprovalTodo(tpmScheduleAdjustment);
        }
        return rows;
    }

    @Transactional
    @Override
    public int cancelByApplicant(Long adjustId, String operator)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentMapper
                .selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!"0".equals(adjustment.getApproveStatus()))
        {
            throw new ServiceException("仅待审的申请可撤销");
        }
        TpmScheduleAdjustment update = new TpmScheduleAdjustment();
        update.setAdjustId(adjustId);
        update.setApproveStatus("3"); // 0待审 1通过 2驳回 3已撤销
        update.setApproveBy(operator);
        update.setApproveTime(new Date());
        update.setApproveComment("申请人撤销");
        update.setUpdateTime(new Date());
        int rows = tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(update);
        // P6：撤销后办结相关待办
        completeAdjustTodos(adjustId);
        return rows;
    }

    @Transactional
    @Override
    public int updateTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setUpdateTime(DateUtils.getNowDate());
        return tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustIds(Long[] adjustIds)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustIds(adjustIds);
    }

    @Transactional
    @Override
    public void approve(Long adjustId, String approveComment)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentMapper
                .selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!"0".equals(adjustment.getApproveStatus()))
        {
            throw new ServiceException("该申请已审批，不能重复审批");
        }

        // 联动回写排课表
        boolean needUpdateSchedule = adjustment.getNewClassroomId() != null
                || adjustment.getNewWeekDay() != null
                || adjustment.getNewStartPeriod() != null
                || adjustment.getNewEndPeriod() != null;
        if (needUpdateSchedule)
        {
            TpmSchedule original = tpmScheduleMapper.selectTpmScheduleByScheduleId(adjustment.getScheduleId());
            if (original == null)
            {
                throw new ServiceException("原排课记录不存在，无法回写");
            }

            Long targetClassroomId = adjustment.getNewClassroomId() != null
                    ? adjustment.getNewClassroomId() : original.getClassroomId();
            Integer targetWeekDay = adjustment.getNewWeekDay() != null
                    ? adjustment.getNewWeekDay() : original.getWeekDay();
            Integer targetStartPeriod = adjustment.getNewStartPeriod() != null
                    ? adjustment.getNewStartPeriod() : original.getStartPeriod();
            Integer targetEndPeriod = adjustment.getNewEndPeriod() != null
                    ? adjustment.getNewEndPeriod() : original.getEndPeriod();

            // 新教室冲突检测
            if (adjustment.getNewClassroomId() != null)
            {
                List<TpmSchedule> conflicts = tpmScheduleMapper.selectByClassroomAndTime(
                        targetClassroomId, targetWeekDay, targetStartPeriod, targetEndPeriod);
                if (conflicts != null)
                {
                    int startWeek = original.getStartWeek() == null ? 1 : original.getStartWeek();
                    int endWeek = original.getEndWeek() == null ? 20 : original.getEndWeek();
                    for (TpmSchedule other : conflicts)
                    {
                        if (other.getScheduleId().equals(original.getScheduleId()))
                        {
                            continue;
                        }
                        int otherStartWeek = other.getStartWeek() == null ? 1 : other.getStartWeek();
                        int otherEndWeek = other.getEndWeek() == null ? 20 : other.getEndWeek();
                        if (TimeSlotUtils.hasWeeksOverlap(startWeek, endWeek, otherStartWeek, otherEndWeek))
                        {
                            throw new ServiceException("审批失败：新教室在该时间段与其他排课冲突");
                        }
                    }
                }
            }

            TpmSchedule update = new TpmSchedule();
            update.setScheduleId(original.getScheduleId());
            if (adjustment.getNewClassroomId() != null)
            {
                update.setClassroomId(adjustment.getNewClassroomId());
            }
            if (adjustment.getNewWeekDay() != null)
            {
                update.setWeekDay(adjustment.getNewWeekDay());
            }
            if (adjustment.getNewStartPeriod() != null)
            {
                update.setStartPeriod(adjustment.getNewStartPeriod());
            }
            if (adjustment.getNewEndPeriod() != null)
            {
                update.setEndPeriod(adjustment.getNewEndPeriod());
            }
            tpmScheduleMapper.updateTpmSchedule(update);
        }

        TpmScheduleAdjustment update = new TpmScheduleAdjustment();
        update.setAdjustId(adjustId);
        update.setApproveStatus("1");
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        update.setApproveComment(approveComment);
        tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(update);

        // O1/P1：调停课审批通过——通知申请人并办结相关待办
        notifyApplicant(adjustment, "调停课申请已通过",
                "您的调停课申请（编号" + adjustId + "）已审批通过，排课已同步更新。意见：" + (approveComment != null ? approveComment : "无"));
        completeAdjustTodos(adjustId);
    }

    @Transactional
    @Override
    public void reject(Long adjustId, String approveComment)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentMapper
                .selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!"0".equals(adjustment.getApproveStatus()))
        {
            throw new ServiceException("该申请已审批，不能重复审批");
        }

        TpmScheduleAdjustment update = new TpmScheduleAdjustment();
        update.setAdjustId(adjustId);
        update.setApproveStatus("2");
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        update.setApproveComment(approveComment);
        tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(update);

        // O1/P1：调停课审批驳回——通知申请人并办结相关待办
        notifyApplicant(adjustment, "调停课申请被驳回",
                "您的调停课申请（编号" + adjustId + "）已被驳回。意见：" + (approveComment != null ? approveComment : "无"));
        completeAdjustTodos(adjustId);
    }

    // ========== O1 消息中心对接（异常不阻断主流程） ==========

    /** 根据用户名解析用户ID（失败返回 null） */
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

    /** 向审批人推送调停课待办+消息（P6：申请提交即产生待办） */
    private void notifyApprovalTodo(TpmScheduleAdjustment adjustment)
    {
        try
        {
            Long receiverId = resolveUserId(adjustApprover);
            if (receiverId == null)
            {
                return;
            }
            String title = "调停课申请待审批（编号" + adjustment.getAdjustId() + "）";
            SysTodo todo = new SysTodo();
            todo.setReceiverId(receiverId);
            todo.setTodoType("1");
            todo.setTitle(title);
            todo.setBusinessType("scheduleAdjust");
            todo.setBusinessId(adjustment.getAdjustId());
            todo.setCreateBy(SecurityUtils.getUsername());
            sysTodoService.createTodo(todo);

            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent("您有一条调停课申请待审批，排课ID=" + adjustment.getScheduleId()
                    + "，类型=" + adjustment.getAdjustType());
            msg.setBusinessType("scheduleAdjust");
            msg.setBusinessId(adjustment.getAdjustId());
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("调停课待办推送失败（adjustId={}）", adjustment.getAdjustId(), e);
        }
    }

    /** 向申请人推送调停课结果消息 */
    private void notifyApplicant(TpmScheduleAdjustment adjustment, String title, String content)
    {
        try
        {
            String applicant = adjustment.getApplicant() != null
                    ? adjustment.getApplicant() : adjustment.getCreateBy();
            Long receiverId = resolveUserId(applicant);
            if (receiverId == null)
            {
                return;
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("2"); // 变更
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType("scheduleAdjust");
            msg.setBusinessId(adjustment.getAdjustId());
            msg.setCreateBy(SecurityUtils.getUsername());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("调停课结果消息推送失败（adjustId={}）", adjustment.getAdjustId(), e);
        }
    }

    /** 办结该调停课申请相关的全部待办 */
    private void completeAdjustTodos(Long adjustId)
    {
        try
        {
            SysTodo query = new SysTodo();
            query.setBusinessType("scheduleAdjust");
            query.setStatus("0");
            List<SysTodo> todos = sysTodoService.selectTodoList(query);
            if (todos != null)
            {
                for (SysTodo t : todos)
                {
                    if (adjustId.equals(t.getBusinessId()))
                    {
                        sysTodoService.completeTodo(t.getTodoId(), t.getReceiverId());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("办结调停课待办失败（adjustId={}）", adjustId, e);
        }
    }
}
