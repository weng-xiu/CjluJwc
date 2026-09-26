package com.yu.oa.workflow.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.oa.workflow.domain.OaCountersignBatch;
import com.yu.oa.workflow.domain.OaCountersignItem;
import com.yu.oa.workflow.mapper.OaCountersignMapper;
import com.yu.oa.workflow.service.IOaCountersignService;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;
import com.yu.system.service.ISysUserService;

/**
 * 通用工作流协同服务实现（加签 / 会签 / 委托）
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@Service
public class OaCountersignServiceImpl implements IOaCountersignService
{
    private static final Logger log = LoggerFactory.getLogger(OaCountersignServiceImpl.class);

    /** 待办业务类型，用于协同待办的创建与办结 */
    private static final String TODO_TYPE = "workflowCoSign";

    @Autowired
    private OaCountersignMapper oaCountersignMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    @Override
    @Transactional
    public Long addSign(String taskId, String operator, String mode, List<String> handlers, String reason)
    {
        if (!MODE_PRE_SIGN.equals(mode) && !MODE_POST_SIGN.equals(mode))
        {
            throw new ServiceException("加签类型不合法");
        }
        Task task = requireOwnedTask(taskId, operator);
        List<String> users = normalizeHandlers(handlers, operator);
        OaCountersignBatch batch = createBatch(task, mode, RULE_ALL, operator, users.size(), reason);
        createItems(batch, users, reason, operator);
        addTaskComment(task, (MODE_PRE_SIGN.equals(mode) ? "前加签：" : "后加签：") + StringUtils.join(users, "、")
                + (StringUtils.isNotEmpty(reason) ? "；说明：" + reason : ""));
        String actionName = MODE_PRE_SIGN.equals(mode) ? "前加签" : "后加签";
        for (String user : users)
        {
            notify(user, "【" + actionName + "】" + batch.getNodeName() + " 待您发表意见",
                    "流程节点「" + batch.getNodeName() + "」由 " + operator + " 发起" + actionName
                            + (StringUtils.isNotEmpty(reason) ? "，说明：" + reason : "")
                            + "，请在流程待办中发表意见。",
                    resolveItemId(batch.getBatchId(), user), true);
        }
        return batch.getBatchId();
    }

    @Override
    @Transactional
    public Long counterSign(String taskId, String operator, List<String> handlers, String rule, String reason)
    {
        String finalRule = StringUtils.isEmpty(rule) ? RULE_ALL : rule.toUpperCase();
        if (!RULE_ALL.equals(finalRule) && !RULE_ANY.equals(finalRule))
        {
            throw new ServiceException("会签规则不合法");
        }
        Task task = requireOwnedTask(taskId, operator);
        List<String> users = normalizeHandlers(handlers, operator);
        if (users.size() < 2)
        {
            throw new ServiceException("会签人数不足，至少需要 2 名非本人办理人");
        }
        OaCountersignBatch batch = createBatch(task, MODE_COUNTER_SIGN, finalRule, operator, users.size(), reason);
        createItems(batch, users, reason, operator);
        addTaskComment(task, "发起会签（" + (RULE_ALL.equals(finalRule) ? "全部同意" : "一人即决") + "）："
                + StringUtils.join(users, "、") + (StringUtils.isNotEmpty(reason) ? "；说明：" + reason : ""));
        for (String user : users)
        {
            notify(user, "【会签】" + batch.getNodeName() + " 待您表决",
                    "流程节点「" + batch.getNodeName() + "」由 " + operator + " 发起会签，共 " + users.size()
                            + " 人表决，规则：" + (RULE_ALL.equals(finalRule) ? "全部同意方为通过" : "任一人同意即定论")
                            + (StringUtils.isNotEmpty(reason) ? "；说明：" + reason : "") + "。",
                    resolveItemId(batch.getBatchId(), user), true);
        }
        return batch.getBatchId();
    }

    @Override
    @Transactional
    public Long delegate(String taskId, String operator, String handler, String reason)
    {
        if (StringUtils.isEmpty(handler))
        {
            throw new ServiceException("请指定代办人");
        }
        if (handler.trim().equals(operator))
        {
            throw new ServiceException("委托对象不能是本人");
        }
        Task task = requireOwnedTask(taskId, operator);
        assertNoHoldBatch(taskId);
        String target = handler.trim();
        if (sysUserService.selectUserByUserName(target) == null)
        {
            throw new ServiceException("委托对象不存在：" + target);
        }
        OaCountersignBatch batch = createBatch(task, MODE_DELEGATE, RULE_ALL, operator, 1, reason);
        createItems(batch, java.util.Collections.singletonList(target), reason, operator);
        // 委托保留 owner 为原办理人，仅移交 assignee，办结后自动知会原办理人
        taskService.setOwner(taskId, operator);
        taskService.setAssignee(taskId, target);
        addTaskComment(task, "委托 " + target + " 代办" + (StringUtils.isNotEmpty(reason) ? "：" + reason : ""));
        notify(target, "【委托代办】" + batch.getNodeName(),
                operator + " 将流程节点「" + batch.getNodeName() + "」委托您代办"
                        + (StringUtils.isNotEmpty(reason) ? "，说明：" + reason : "")
                        + "，办理完成后系统会自动知会原办理人。",
                resolveItemId(batch.getBatchId(), target), true);
        return batch.getBatchId();
    }

    @Override
    @Transactional
    public void reclaimDelegate(String taskId, String operator, String reason)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            throw new ServiceException("任务不存在或已办结");
        }
        if (!operator.equals(task.getOwner()) && !operator.equals(task.getAssignee()))
        {
            throw new ServiceException("仅原办理人可收回委托");
        }
        List<OaCountersignBatch> batches = oaCountersignMapper.selectActiveBatchByTaskId(taskId);
        boolean reclaimed = false;
        for (OaCountersignBatch batch : batches)
        {
            if (!MODE_DELEGATE.equals(batch.getMode()))
            {
                continue;
            }
            closeBatch(batch.getBatchId());
            cancelPendingItems(batch.getBatchId(), operator);
            taskService.setAssignee(taskId, operator);
            taskService.setOwner(taskId, null);
            addTaskComment(task, "收回委托" + (StringUtils.isNotEmpty(reason) ? "：" + reason : ""));
            for (OaCountersignItem item : oaCountersignMapper.selectItemsByBatchId(batch.getBatchId()))
            {
                closeTodo(item.getHandler(), resolveItemId(batch.getBatchId(), item.getHandler()));
            }
            reclaimed = true;
        }
        if (!reclaimed)
        {
            throw new ServiceException("该任务当前不存在委托");
        }
    }

    @Override
    @Transactional
    public void submitOpinion(Long itemId, String operator, boolean agree, String opinion)
    {
        OaCountersignItem item = oaCountersignMapper.selectItemById(itemId);
        if (item == null)
        {
            throw new ServiceException("协同意见不存在");
        }
        if (!operator.equals(item.getHandler()))
        {
            throw new ServiceException("该意见不属于当前用户");
        }
        if (!"0".equals(item.getStatus()))
        {
            throw new ServiceException("意见已提交，不可重复表态");
        }
        OaCountersignBatch batch = oaCountersignMapper.selectBatchById(item.getBatchId());
        if (batch == null || !"0".equals(batch.getStatus()))
        {
            throw new ServiceException("所属协同已取消或已结束");
        }
        OaCountersignItem update = new OaCountersignItem();
        update.setItemId(itemId);
        update.setVote(agree ? "1" : "2");
        update.setOpinion(opinion);
        update.setStatus("1");
        update.setHandleTime(DateUtils.getNowDate());
        update.setUpdateBy(operator);
        oaCountersignMapper.updateItem(update);

        addTaskCommentById(batch.getTaskId(), batch.getProcessInstanceId(),
                (MODE_COUNTER_SIGN.equals(batch.getMode()) ? "会签表决" : "加签意见") + "（" + operator + "）："
                        + (agree ? "同意" : "不同意") + (StringUtils.isNotEmpty(opinion) ? " " + opinion : ""));
        closeTodo(operator, itemId);

        refreshBatchCounters(batch);
        OaCountersignBatch latest = oaCountersignMapper.selectBatchById(batch.getBatchId());
        if (isSatisfied(latest))
        {
            OaCountersignBatch release = new OaCountersignBatch();
            release.setBatchId(latest.getBatchId());
            release.setStatus("1");
            release.setReleaseTime(DateUtils.getNowDate());
            release.setUpdateBy(operator);
            oaCountersignMapper.updateBatch(release);
            notify(latest.getInitiator(),
                    "【协同意见已齐】" + latest.getNodeName(),
                    "节点「" + latest.getNodeName() + "」的" + modeName(latest.getMode()) + "意见已齐："
                            + "同意 " + latest.getAgreeCount() + " 人、不同意 " + latest.getDisagreeCount()
                            + " 人（共 " + latest.getTotalCount() + " 人），请继续提交处理结果。",
                    latest.getBatchId(), false);
        }
    }

    @Override
    public List<OaCountersignItem> listMyPending(String handler)
    {
        return oaCountersignMapper.selectPendingItemsByHandler(handler);
    }

    @Override
    public List<OaCountersignItem> listByProcessInstance(String processInstanceId)
    {
        return oaCountersignMapper.selectItemsByProcessInstanceId(processInstanceId);
    }

    @Override
    public List<Map<String, Object>> listHandlerOptions()
    {
        return oaCountersignMapper.selectHandlerOptions();
    }

    @Override
    public void assertGatePassed(String taskId)
    {
        List<OaCountersignBatch> batches = oaCountersignMapper.selectActiveBatchByTaskId(taskId);
        for (OaCountersignBatch batch : batches)
        {
            boolean blocking = MODE_PRE_SIGN.equals(batch.getMode()) || MODE_COUNTER_SIGN.equals(batch.getMode());
            if (!blocking)
            {
                continue;
            }
            int pending = oaCountersignMapper.countPendingByTaskAndMode(taskId, batch.getMode());
            if (pending > 0)
            {
                throw new ServiceException("该节点存在未回复的" + modeName(batch.getMode()) + "意见（" + pending
                        + " 条待表态），请等待意见返回后再提交");
            }
        }
    }

    @Override
    @Transactional
    public void afterTaskCompleted(String taskId, String operator)
    {
        List<OaCountersignBatch> batches = oaCountersignMapper.selectActiveBatchByTaskId(taskId);
        for (OaCountersignBatch batch : batches)
        {
            closeBatch(batch.getBatchId());
            if (MODE_DELEGATE.equals(batch.getMode()))
            {
                // 委托办结：知会保留为 owner 的原办理人
                notify(batch.getInitiator(), "【委托已办结】" + batch.getNodeName(),
                        operator + " 已代办完成流程节点「" + batch.getNodeName() + "」。",
                        batch.getBatchId(), false);
            }
            // 前加签/后加签/会签未表态意见随任务办结作废，清理其待办
            List<OaCountersignItem> items = oaCountersignMapper.selectItemsByBatchId(batch.getBatchId());
            for (OaCountersignItem item : items)
            {
                if ("0".equals(item.getStatus()))
                {
                    OaCountersignItem cancel = new OaCountersignItem();
                    cancel.setItemId(item.getItemId());
                    cancel.setStatus("2");
                    cancel.setUpdateBy(operator);
                    oaCountersignMapper.updateItem(cancel);
                    closeTodo(item.getHandler(), item.getItemId());
                }
            }
        }
    }

    @Override
    @Transactional
    public void cancelByProcessInstance(String processInstanceId)
    {
        oaCountersignMapper.cancelBatchByProcessInstanceId(processInstanceId);
    }

    // ------------------------------------------------------------------ 内部方法

    /**
     * 校验任务存在且由操作人办理
     */
    private Task requireOwnedTask(String taskId, String operator)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null)
        {
            throw new ServiceException("任务不存在或已办结");
        }
        if (!operator.equals(task.getAssignee()))
        {
            throw new ServiceException("当前任务不在您的待办中，无法发起协同");
        }
        return task;
    }

    /**
     * 同一任务允许并存前加签与后加签，但委托与协同互斥（避免办理人归属歧义）
     */
    private void assertNoHoldBatch(String taskId)
    {
        for (OaCountersignBatch batch : oaCountersignMapper.selectActiveBatchByTaskId(taskId))
        {
            if (MODE_DELEGATE.equals(batch.getMode()))
            {
                throw new ServiceException("该任务已处于委托代办中");
            }
        }
    }

    /**
     * 去空、去重、剔除本人，并校验用户存在
     */
    private List<String> normalizeHandlers(List<String> handlers, String operator)
    {
        if (handlers == null || handlers.isEmpty())
        {
            throw new ServiceException("请至少选择一名协同办理人");
        }
        Set<String> set = new LinkedHashSet<>();
        for (String handler : handlers)
        {
            if (StringUtils.isEmpty(handler))
            {
                continue;
            }
            String user = handler.trim();
            if (user.equals(operator))
            {
                continue;
            }
            if (sysUserService.selectUserByUserName(user) == null)
            {
                throw new ServiceException("协同办理人不存在：" + user);
            }
            set.add(user);
        }
        if (set.isEmpty())
        {
            throw new ServiceException("协同办理人不能仅为本人");
        }
        return new ArrayList<>(set);
    }

    private OaCountersignBatch createBatch(Task task, String mode, String rule, String initiator, int total, String reason)
    {
        assertNoHoldBatchIfDelegate(task.getId(), mode);
        OaCountersignBatch batch = new OaCountersignBatch();
        batch.setTaskId(task.getId());
        batch.setProcessInstanceId(task.getProcessInstanceId());
        batch.setProcessDefinitionName(resolveProcessName(task.getProcessInstanceId()));
        batch.setNodeId(task.getTaskDefinitionKey());
        batch.setNodeName(task.getName());
        batch.setMode(mode);
        batch.setRule(rule);
        batch.setInitiator(initiator);
        batch.setTotalCount(total);
        batch.setDoneCount(0);
        batch.setAgreeCount(0);
        batch.setDisagreeCount(0);
        batch.setStatus("0");
        batch.setCreateBy(initiator);
        batch.setCreateTime(DateUtils.getNowDate());
        batch.setRemark(reason);
        oaCountersignMapper.insertBatch(batch);
        return batch;
    }

    /**
     * 前加签/会签进行中不允许再委托，防止办理权归属歧义
     */
    private void assertNoHoldBatchIfDelegate(String taskId, String mode)
    {
        if (!MODE_DELEGATE.equals(mode))
        {
            return;
        }
        assertNoHoldBatch(taskId);
    }

    private void createItems(OaCountersignBatch batch, List<String> users, String reason, String operator)
    {
        for (String user : users)
        {
            OaCountersignItem item = new OaCountersignItem();
            item.setBatchId(batch.getBatchId());
            item.setTaskId(batch.getTaskId());
            item.setProcessInstanceId(batch.getProcessInstanceId());
            item.setHandler(user);
            item.setHandlerName(resolveNickName(user));
            item.setVote("0");
            item.setStatus("0");
            item.setCreateBy(operator);
            item.setCreateTime(DateUtils.getNowDate());
            item.setRemark(reason);
            oaCountersignMapper.insertItem(item);
        }
    }

    private void refreshBatchCounters(OaCountersignBatch batch)
    {
        int done = 0;
        int agree = 0;
        int disagree = 0;
        for (OaCountersignItem item : oaCountersignMapper.selectItemsByBatchId(batch.getBatchId()))
        {
            if (!"1".equals(item.getStatus()))
            {
                continue;
            }
            done++;
            if ("1".equals(item.getVote()))
            {
                agree++;
            }
            else if ("2".equals(item.getVote()))
            {
                disagree++;
            }
        }
        OaCountersignBatch update = new OaCountersignBatch();
        update.setBatchId(batch.getBatchId());
        update.setDoneCount(done);
        update.setAgreeCount(agree);
        update.setDisagreeCount(disagree);
        update.setUpdateTime(DateUtils.getNowDate());
        oaCountersignMapper.updateBatch(update);
    }

    /**
     * ALL：全部表态后齐备；ANY：出现同意或全部不同意即定论
     */
    private boolean isSatisfied(OaCountersignBatch batch)
    {
        if (batch == null || !"0".equals(batch.getStatus()))
        {
            return false;
        }
        int total = batch.getTotalCount() == null ? 0 : batch.getTotalCount();
        int done = batch.getDoneCount() == null ? 0 : batch.getDoneCount();
        int agree = batch.getAgreeCount() == null ? 0 : batch.getAgreeCount();
        if (RULE_ANY.equals(batch.getRule()))
        {
            return agree > 0 || done >= total;
        }
        return done >= total;
    }

    private void closeBatch(Long batchId)
    {
        OaCountersignBatch update = new OaCountersignBatch();
        update.setBatchId(batchId);
        update.setStatus("1");
        update.setReleaseTime(DateUtils.getNowDate());
        oaCountersignMapper.updateBatch(update);
    }

    private void cancelPendingItems(Long batchId, String operator)
    {
        for (OaCountersignItem item : oaCountersignMapper.selectItemsByBatchId(batchId))
        {
            if ("0".equals(item.getStatus()))
            {
                OaCountersignItem cancel = new OaCountersignItem();
                cancel.setItemId(item.getItemId());
                cancel.setStatus("2");
                cancel.setUpdateBy(operator);
                oaCountersignMapper.updateItem(cancel);
            }
        }
    }

    /**
     * 按批次取指定办理人的明细ID（作为待办业务主键，便于精确办结）
     */
    private Long resolveItemId(Long batchId, String handler)
    {
        for (OaCountersignItem item : oaCountersignMapper.selectItemsByBatchId(batchId))
        {
            if (handler.equals(item.getHandler()))
            {
                return item.getItemId();
            }
        }
        return batchId;
    }

    private String resolveProcessName(String processInstanceId)
    {
        if (StringUtils.isEmpty(processInstanceId))
        {
            return null;
        }
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        return hpi == null ? null : hpi.getProcessDefinitionName();
    }

    private String resolveNickName(String userName)
    {
        com.yu.common.core.domain.entity.SysUser user = sysUserService.selectUserByUserName(userName);
        return user == null ? userName : StringUtils.nvl(user.getNickName(), userName);
    }

    private Long resolveUserId(String userName)
    {
        com.yu.common.core.domain.entity.SysUser user = sysUserService.selectUserByUserName(userName);
        return user == null ? null : user.getUserId();
    }

    private void addTaskComment(Task task, String message)
    {
        addTaskCommentById(task.getId(), task.getProcessInstanceId(), message);
    }

    private void addTaskCommentById(String taskId, String processInstanceId, String message)
    {
        try
        {
            taskService.addComment(taskId, processInstanceId, message);
        }
        catch (Exception e)
        {
            log.warn("写入流程意见留痕失败 taskId={}：{}", taskId, e.getMessage());
        }
    }

    /**
     * 推送站内消息，withTodo 时同步生成待办
     */
    private void notify(String userName, String title, String content, Long businessId, boolean withTodo)
    {
        try
        {
            Long receiverId = resolveUserId(userName);
            if (receiverId == null)
            {
                return;
            }
            if (withTodo)
            {
                SysTodo todo = new SysTodo();
                todo.setReceiverId(receiverId);
                todo.setTodoType("1");
                todo.setTitle(title);
                todo.setBusinessType(TODO_TYPE);
                todo.setBusinessId(businessId);
                todo.setCreateBy("system");
                sysTodoService.createTodo(todo);
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(receiverId);
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType(TODO_TYPE);
            msg.setBusinessId(businessId);
            msg.setCreateBy("system");
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("工作流协同通知推送失败（user={}, title={}）", userName, title, e);
        }
    }

    /**
     * 办结指定明细对应的协同待办
     */
    private void closeTodo(String userName, Long businessId)
    {
        try
        {
            Long receiverId = resolveUserId(userName);
            if (receiverId == null || businessId == null)
            {
                return;
            }
            SysTodo query = new SysTodo();
            query.setReceiverId(receiverId);
            query.setBusinessType(TODO_TYPE);
            query.setStatus("0");
            for (SysTodo todo : sysTodoService.selectTodoList(query))
            {
                if (businessId.equals(todo.getBusinessId()))
                {
                    sysTodoService.completeTodo(todo.getTodoId(), receiverId);
                }
            }
        }
        catch (Exception e)
        {
            log.warn("办结协同待办失败 user={} itemId={}：{}", userName, businessId, e.getMessage());
        }
    }

    private String modeName(String mode)
    {
        if (MODE_PRE_SIGN.equals(mode))
        {
            return "前加签";
        }
        if (MODE_POST_SIGN.equals(mode))
        {
            return "后加签";
        }
        if (MODE_COUNTER_SIGN.equals(mode))
        {
            return "会签";
        }
        if (MODE_DELEGATE.equals(mode))
        {
            return "委托";
        }
        return "协同";
    }
}
