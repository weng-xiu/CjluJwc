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
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.utils.DateUtils;
import com.yu.oa.domain.OaProcessInstance;
import com.yu.oa.domain.OaTaskRecord;
import com.yu.oa.mapper.OaProcessInstanceMapper;
import com.yu.oa.mapper.OaTaskRecordMapper;
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
    public List<Map<String, Object>> listProcessInstances()
    {
        List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery()
                .orderByProcessInstanceStartTime().desc()
                .list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricProcessInstance hpi : list)
        {
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
    @Transactional
    public ProcessInstance startProcessInstance(String processKey, String businessKey, Map<String, Object> variables)
    {
        return runtimeService.startProcessInstanceByKey(processKey, businessKey, variables);
    }

    @Override
    public List<Task> listTodoTasks(String assignee)
    {
        return taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime().desc()
                .list();
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
        if (comment != null && !comment.trim().isEmpty())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), comment);
        }
        if (variables != null && !variables.isEmpty())
        {
            taskService.setVariablesLocal(taskId, variables);
        }
        taskService.complete(taskId, variables);
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
        // 简单实现：通过设置 rejected 变量，由 BPMN 条件分支决定回退路径
        taskService.setVariable(taskId, "rejected", true);
        taskService.setVariable(taskId, "rejectComment", comment);
        if (comment != null && !comment.trim().isEmpty())
        {
            taskService.addComment(taskId, task.getProcessInstanceId(), "驳回：" + comment);
        }
        taskService.complete(taskId);
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
