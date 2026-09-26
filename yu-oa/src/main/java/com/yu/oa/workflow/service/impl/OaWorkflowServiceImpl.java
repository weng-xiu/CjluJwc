package com.yu.oa.workflow.service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.domain.OaTaskRecord;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.mapper.OaTaskRecordMapper;
import com.yu.oa.workflow.service.IOaCountersignService;
import com.yu.oa.workflow.service.IOaWorkflowService;

/**
 * OA 工作流服务实现
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaWorkflowServiceImpl implements IOaWorkflowService
{
    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private OaProcessInstanceMapper oaProcessInstanceMapper;

    @Autowired
    private OaTaskRecordMapper oaTaskRecordMapper;

    @Autowired
    private IOaCountersignService oaCountersignService;

    @Override
    @Transactional
    public Deployment deployProcess(String processName, InputStream bpmnInputStream)
    {
        return repositoryService.createDeployment()
                .name(processName)
                .addInputStream(processName + ".bpmn20.xml", bpmnInputStream)
                .deploy();
    }

    @Override
    @Transactional
    public Deployment deployProcess(String processName, String bpmnXml)
    {
        return deployProcess(processName, new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public List<ProcessDefinition> listProcessDefinitions()
    {
        return repositoryService.createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().desc()
                .list();
    }

    @Override
    public List<Map<String, Object>> listProcessInstances(String processDefinitionName, String startUserId, String status)
    {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if (StringUtils.isNotEmpty(startUserId))
        {
            query.startedBy(startUserId);
        }
        if ("running".equals(status))
        {
            query.unfinished();
        }
        else if ("finished".equals(status))
        {
            query.finished();
        }
        List<HistoricProcessInstance> list = query.orderByProcessInstanceStartTime().desc().list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricProcessInstance hpi : list)
        {
            // Flowable 仅支持流程名称精确查询，模糊匹配在内存中过滤
            if (StringUtils.isNotEmpty(processDefinitionName)
                    && (hpi.getProcessDefinitionName() == null
                        || !hpi.getProcessDefinitionName().contains(processDefinitionName)))
            {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("processInstanceId", hpi.getId());
            map.put("processDefinitionId", hpi.getProcessDefinitionId());
            map.put("processDefinitionName", hpi.getProcessDefinitionName());
            map.put("businessKey", hpi.getBusinessKey());
            map.put("startUserId", hpi.getStartUserId());
            map.put("startTime", hpi.getStartTime());
            map.put("endTime", hpi.getEndTime());
            map.put("durationInMillis", hpi.getDurationInMillis());
            map.put("deleteReason", hpi.getDeleteReason());
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getProcessInstanceDetail(String processInstanceId)
    {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hpi == null)
        {
            return null;
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("processInstanceId", hpi.getId());
        detail.put("processDefinitionId", hpi.getProcessDefinitionId());
        detail.put("processDefinitionName", hpi.getProcessDefinitionName());
        detail.put("businessKey", hpi.getBusinessKey());
        detail.put("startUserId", hpi.getStartUserId());
        detail.put("startTime", hpi.getStartTime());
        detail.put("endTime", hpi.getEndTime());
        detail.put("durationInMillis", hpi.getDurationInMillis());
        detail.put("deleteReason", hpi.getDeleteReason());
        // 历史任务链（含进行中任务）及各任务审批意见
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (HistoricTaskInstance hti : tasks)
        {
            Map<String, Object> t = new HashMap<>();
            t.put("taskId", hti.getId());
            t.put("taskName", hti.getName());
            t.put("assignee", hti.getAssignee());
            t.put("startTime", hti.getStartTime());
            t.put("endTime", hti.getEndTime());
            t.put("durationInMillis", hti.getDurationInMillis());
            List<String> comments = new ArrayList<>();
            for (Comment c : taskService.getTaskComments(hti.getId()))
            {
                comments.add(c.getFullMessage());
            }
            t.put("comments", comments);
            taskList.add(t);
        }
        detail.put("tasks", taskList);
        // O1 协同留痕：加签/会签/委托意见随流程详情一并返回，供各业务追溯页展示
        detail.put("countersigns", oaCountersignService.listByProcessInstance(processInstanceId));
        return detail;
    }

    @Override
    @Transactional
    public ProcessInstance startProcessInstance(String processKey, String businessKey, Map<String, Object> variables)
    {
        return runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
    }

    @Override
    public List<Task> listTodoTasks(String assignee, String taskName)
    {
        TaskQuery query = taskService.createTaskQuery().taskAssignee(assignee);
        if (StringUtils.isNotEmpty(taskName))
        {
            query.taskNameLike("%" + taskName + "%");
        }
        return query.orderByTaskCreateTime().desc().list();
    }

    @Override
    public List<HistoricTaskInstance> listDoneTasks(String assignee, String taskName)
    {
        HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(assignee).finished();
        if (StringUtils.isNotEmpty(taskName))
        {
            query.taskNameLike("%" + taskName + "%");
        }
        return query.orderByHistoricTaskInstanceEndTime().desc().list();
    }

    @Override
    @Transactional
    public void completeTask(String taskId, String assignee, Map<String, Object> variables, String comment)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).taskAssignee(assignee).singleResult();
        if (task == null)
        {
            throw new RuntimeException("当前任务不存在或无权处理");
        }
        // 前加签/会签意见未齐时拒绝提交，保证协同结论先于节点办结
        oaCountersignService.assertGatePassed(taskId);
        if (comment != null && !comment.trim().isEmpty())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        if (variables != null && !variables.isEmpty())
        {
            taskService.setVariablesLocal(taskId, variables);
        }
        taskService.complete(taskId, variables);
        oaCountersignService.afterTaskCompleted(taskId, assignee);
    }

    @Override
    @Transactional
    public void rejectTask(String taskId, String assignee, String comment)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).taskAssignee(assignee).singleResult();
        if (task == null)
        {
            throw new RuntimeException("当前任务不存在或无权处理");
        }
        oaCountersignService.assertGatePassed(taskId);
        // 简单实现：通过设置 rejected 变量，由 BPMN 条件分支决定回退路径
        taskService.setVariable(taskId, "rejected", true);
        taskService.setVariable(taskId, "rejectComment", comment);
        // 已接入的四个流程均按 ${approved == false} 判断驳回分支，否则排他网关无可选出线
        taskService.setVariable(taskId, "approved", false);
        if (comment != null && !comment.trim().isEmpty())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), "驳回：" + comment);
        }
        taskService.complete(taskId);
        oaCountersignService.afterTaskCompleted(taskId, assignee);
    }

    @Override
    @Transactional
    public void transferTask(String taskId, String originalAssignee, String newAssignee, String comment)
    {
        Task task = taskService.createTaskQuery().taskId(taskId).taskAssignee(originalAssignee).singleResult();
        if (task == null)
        {
            throw new RuntimeException("当前任务不存在或无权处理");
        }
        taskService.setAssignee(taskId, newAssignee);
        if (comment != null && !comment.trim().isEmpty())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), "转办给 " + newAssignee + "：" + comment);
        }
    }

    @Override
    @Transactional
    public void cancelProcessInstance(String processInstanceId, String reason)
    {
        runtimeService.deleteProcessInstance(processInstanceId, reason);
        // 实例终止后同步作废进行中的加签/会签/委托批次
        oaCountersignService.cancelByProcessInstance(processInstanceId);
    }

    @Override
    public List<OaTaskRecord> listTaskRecords(Long instanceId)
    {
        return oaTaskRecordMapper.selectOaTaskRecordByInstanceId(instanceId);
    }

    @Override
    public String getProcessBpmnXml(String definitionId)
    {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(definitionId).singleResult();
        if (processDefinition == null)
        {
            return null;
        }
        InputStream inputStream = repositoryService.getResourceAsStream(
                processDefinition.getDeploymentId(), processDefinition.getResourceName());
        try
        {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new RuntimeException("读取流程图失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteDeployment(String deploymentId)
    {
        repositoryService.deleteDeployment(deploymentId, true);
    }
}
