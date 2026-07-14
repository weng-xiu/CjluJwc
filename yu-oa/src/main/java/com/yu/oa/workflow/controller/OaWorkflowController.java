package com.yu.oa.workflow.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.workflow.service.IOaWorkflowService;

/**
 * OA 工作流管理 Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/workflow")
public class OaWorkflowController extends BaseController
{
    @Autowired
    private IOaWorkflowService oaWorkflowService;

    /**
     * 上传并部署 BPMN 流程文件
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:deploy')")
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PostMapping("/deploy")
    public AjaxResult deploy(@RequestParam("file") MultipartFile file) throws IOException
    {
        String fileName = file.getOriginalFilename();
        Deployment deployment = oaWorkflowService.deployProcess(fileName, file.getInputStream());
        return success(deployment.getId());
    }

    /**
     * 查询流程定义列表
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:list')")
    @GetMapping("/definition/list")
    public TableDataInfo definitionList()
    {
        startPage();
        List<ProcessDefinition> list = oaWorkflowService.listProcessDefinitions();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessDefinition pd : list)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pd.getId());
            map.put("key", pd.getKey());
            map.put("name", pd.getName());
            map.put("version", pd.getVersion());
            map.put("deploymentId", pd.getDeploymentId());
            map.put("suspended", pd.isSuspended());
            map.put("description", pd.getDescription());
            result.add(map);
        }
        return getDataTable(result);
    }

    /**
     * 获取流程图 XML
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:query')")
    @GetMapping("/definition/{definitionId}/xml")
    public AjaxResult getBpmnXml(@PathVariable("definitionId") String definitionId)
    {
        return success(oaWorkflowService.getProcessBpmnXml(definitionId));
    }

    /**
     * 查询流程实例列表
     */
    @PreAuthorize("@ss.hasPermi('oa:instance:list')")
    @GetMapping("/instance/list")
    public TableDataInfo instanceList()
    {
        startPage();
        List<Map<String, Object>> list = oaWorkflowService.listProcessInstances();
        return getDataTable(list);
    }

    /**
     * 查询当前用户待办任务
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/task/todo")
    public TableDataInfo todoList()
    {
        startPage();
        String assignee = SecurityUtils.getUsername();
        List<Task> list = oaWorkflowService.listTodoTasks(assignee);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : list)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("taskName", task.getName());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("processDefinitionId", task.getProcessDefinitionId());
            map.put("createTime", task.getCreateTime());
            map.put("assignee", task.getAssignee());
            result.add(map);
        }
        return getDataTable(result);
    }

    /**
     * 完成任务
     */
    @PreAuthorize("@ss.hasPermi('oa:task:approve')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/complete/{taskId}")
    public AjaxResult complete(@PathVariable("taskId") String taskId,
                               @RequestBody(required = false) Map<String, Object> params)
    {
        String assignee = SecurityUtils.getUsername();
        String comment = params != null ? (String) params.get("comment") : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = params != null ? (Map<String, Object>) params.get("variables") : null;
        oaWorkflowService.completeTask(taskId, assignee, variables, comment);
        return success();
    }

    /**
     * 驳回任务
     */
    @PreAuthorize("@ss.hasPermi('oa:task:approve')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/reject/{taskId}")
    public AjaxResult reject(@PathVariable("taskId") String taskId,
                             @RequestBody(required = false) Map<String, String> params)
    {
        String assignee = SecurityUtils.getUsername();
        String comment = params != null ? params.get("comment") : null;
        oaWorkflowService.rejectTask(taskId, assignee, comment);
        return success();
    }

    /**
     * 转办任务
     */
    @PreAuthorize("@ss.hasPermi('oa:task:approve')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/transfer/{taskId}")
    public AjaxResult transfer(@PathVariable("taskId") String taskId,
                               @RequestBody Map<String, String> params)
    {
        String originalAssignee = SecurityUtils.getUsername();
        String newAssignee = params.get("newAssignee");
        String comment = params.get("comment");
        oaWorkflowService.transferTask(taskId, originalAssignee, newAssignee, comment);
        return success();
    }

    /**
     * 删除部署
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:remove')")
    @Log(title = "流程定义", businessType = BusinessType.DELETE)
    @PostMapping("/definition/delete/{deploymentId}")
    public AjaxResult deleteDeployment(@PathVariable("deploymentId") String deploymentId)
    {
        oaWorkflowService.deleteDeployment(deploymentId);
        return success();
    }
}
