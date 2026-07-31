package com.yu.oa.workflow.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

import com.yu.oa.domain.OaProcessDefinition;
import com.yu.oa.domain.OaTaskRecord;

/**
 * OA 工作流服务接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaWorkflowService
{
    /**
     * 部署流程定义
     */
    public Deployment deployProcess(String processName, InputStream bpmnInputStream);

    /**
     * 根据 BPMN XML 部署流程
     */
    public Deployment deployProcess(String processName, String bpmnXml);

    /**
     * 查询流程定义列表
     */
    public List<ProcessDefinition> listProcessDefinitions();

    /**
     * 查询流程实例列表（支持按流程名称、发起人、状态筛选）
     * 
     * @param processDefinitionName 流程名称（模糊）
     * @param startUserId 发起人
     * @param status 状态：running 运行中 / finished 已结束，空为全部
     */
    public List<Map<String, Object>> listProcessInstances(String processDefinitionName, String startUserId, String status);

    /**
     * 获取流程实例详情（含历史任务与审批意见）
     */
    public Map<String, Object> getProcessInstanceDetail(String processInstanceId);

    /**
     * 启动流程实例
     * 
     * @param processKey 流程标识
     * @param businessKey 业务键，格式：businessType:businessId
     * @param variables 流程变量
     */
    public ProcessInstance startProcessInstance(String processKey, String businessKey, Map<String, Object> variables);

    /**
     * 查询待办任务（支持按任务名称模糊筛选）
     */
    public List<Task> listTodoTasks(String assignee, String taskName);

    /**
     * 完成任务
     */
    public void completeTask(String taskId, String assignee, Map<String, Object> variables, String comment);

    /**
     * 驳回任务（通过设置变量回到上一节点，具体依赖 BPMN 设计）
     */
    public void rejectTask(String taskId, String assignee, String comment);

    /**
     * 转办任务
     */
    public void transferTask(String taskId, String originalAssignee, String newAssignee, String comment);

    /**
     * 撤销流程实例
     */
    public void cancelProcessInstance(String processInstanceId, String reason);

    /**
     * 获取流程任务历史
     */
    public List<OaTaskRecord> listTaskRecords(Long instanceId);

    /**
     * 获取流程图 XML
     */
    public String getProcessBpmnXml(String definitionId);

    /**
     * 删除部署
     */
    public void deleteDeployment(String deploymentId);
}
