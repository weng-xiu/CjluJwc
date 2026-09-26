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
import org.flowable.task.api.history.HistoricTaskInstance;
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
import com.yu.oa.workflow.domain.OaCountersignItem;
import com.yu.oa.workflow.service.IOaCountersignService;
import com.yu.oa.workflow.service.IOaProcessDiagramService;
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

    @Autowired
    private IOaCountersignService oaCountersignService;

    @Autowired
    private IOaProcessDiagramService oaProcessDiagramService;

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
     * 获取流程图 XML（definitionId 含特殊字符，用 RequestParam 传递）
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:query')")
    @GetMapping("/definition/xml")
    public AjaxResult getBpmnXml(@RequestParam("definitionId") String definitionId)
    {
        String bpmnXml = oaWorkflowService.getProcessBpmnXml(definitionId);
        if (bpmnXml == null)
        {
            return error("流程定义不存在或已被删除");
        }
        // 注意：不能直接 success(bpmnXml)，会命中 success(String message) 重载导致 XML 被当作 msg
        return AjaxResult.success("操作成功", bpmnXml);
    }

    /**
     * 查询流程实例列表（支持按流程名称、发起人、状态筛选）
     */
    @PreAuthorize("@ss.hasPermi('oa:instance:list')")
    @GetMapping("/instance/list")
    public TableDataInfo instanceList(@RequestParam(value = "processDefinitionName", required = false) String processDefinitionName,
                                      @RequestParam(value = "startUserId", required = false) String startUserId,
                                      @RequestParam(value = "status", required = false) String status)
    {
        startPage();
        List<Map<String, Object>> list = oaWorkflowService.listProcessInstances(processDefinitionName, startUserId, status);
        return getDataTable(list);
    }

    /**
     * 获取流程实例详情（含历史任务与审批意见，ID含特殊字符用 RequestParam 传递；待办任务页详情也复用此接口）
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:instance:query,oa:task:list')")
    @GetMapping("/instance/detail")
    public AjaxResult instanceDetail(@RequestParam("processInstanceId") String processInstanceId)
    {
        Map<String, Object> detail = oaWorkflowService.getProcessInstanceDetail(processInstanceId);
        if (detail == null)
        {
            return error("流程实例不存在或已被删除");
        }
        return success(detail);
    }

    /**
     * 终止运行中的流程实例
     */
    @PreAuthorize("@ss.hasPermi('oa:instance:cancel')")
    @Log(title = "流程实例", businessType = BusinessType.DELETE)
    @PostMapping("/instance/cancel")
    public AjaxResult cancelInstance(@RequestBody Map<String, String> params)
    {
        String processInstanceId = params.get("processInstanceId");
        if (processInstanceId == null || processInstanceId.trim().isEmpty())
        {
            return error("流程实例ID不能为空");
        }
        String reason = params.get("reason");
        oaWorkflowService.cancelProcessInstance(processInstanceId,
                (reason == null || reason.trim().isEmpty()) ? "管理员终止" : reason);
        return success();
    }

    /**
     * 查询当前用户待办任务（支持按任务名称模糊筛选）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/task/todo")
    public TableDataInfo todoList(@RequestParam(value = "taskName", required = false) String taskName)
    {
        startPage();
        String assignee = SecurityUtils.getUsername();
        List<Task> list = oaWorkflowService.listTodoTasks(assignee, taskName);
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
     * 查询当前用户已办任务
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/task/done")
    public TableDataInfo doneList(@RequestParam(value = "taskName", required = false) String taskName)
    {
        startPage();
        String assignee = SecurityUtils.getUsername();
        List<HistoricTaskInstance> list = oaWorkflowService.listDoneTasks(assignee, taskName);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HistoricTaskInstance hti : list)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", hti.getId());
            map.put("taskName", hti.getName());
            map.put("processInstanceId", hti.getProcessInstanceId());
            map.put("processDefinitionId", hti.getProcessDefinitionId());
            map.put("assignee", hti.getAssignee());
            map.put("startTime", hti.getStartTime());
            map.put("endTime", hti.getEndTime());
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
     * 获取流程图渲染数据（节点/连线/当前进度，服务端自动布局）
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:instance:query,oa:instance:list,oa:task:list')")
    @GetMapping("/diagram")
    public AjaxResult diagram(@RequestParam("processInstanceId") String processInstanceId)
    {
        Object diagram = oaProcessDiagramService.buildDiagram(processInstanceId);
        if (diagram == null)
        {
            return error("流程实例不存在或已被删除");
        }
        return success(diagram);
    }

    /**
     * 加签：mode 0 前加签（意见未齐不可提交）/ 1 后加签（不阻塞本人提交）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:coSign')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/addSign/{taskId}")
    public AjaxResult addSign(@PathVariable("taskId") String taskId,
                             @RequestBody Map<String, Object> params)
    {
        String mode = params.get("mode") == null ? "0" : String.valueOf(params.get("mode"));
        String reason = params.get("reason") == null ? null : String.valueOf(params.get("reason"));
        Long batchId = oaCountersignService.addSign(taskId, SecurityUtils.getUsername(), mode,
                extractHandlers(params), reason);
        return AjaxResult.success("加签发起成功", batchId);
    }

    /**
     * 会签：多人并行表决，rule ALL 全部同意 / ANY 一人同意即定论
     */
    @PreAuthorize("@ss.hasPermi('oa:task:coSign')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/counterSign/{taskId}")
    public AjaxResult counterSign(@PathVariable("taskId") String taskId,
                                 @RequestBody Map<String, Object> params)
    {
        String rule = params.get("rule") == null ? "ALL" : String.valueOf(params.get("rule"));
        String reason = params.get("reason") == null ? null : String.valueOf(params.get("reason"));
        Long batchId = oaCountersignService.counterSign(taskId, SecurityUtils.getUsername(),
                extractHandlers(params), rule, reason);
        return AjaxResult.success("会签发起成功", batchId);
    }

    /**
     * 委托代办：任务移交他人办理，原办理人保留为 owner
     */
    @PreAuthorize("@ss.hasPermi('oa:task:coSign')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/delegate/{taskId}")
    public AjaxResult delegate(@PathVariable("taskId") String taskId,
                              @RequestBody Map<String, String> params)
    {
        Long batchId = oaCountersignService.delegate(taskId, SecurityUtils.getUsername(),
                params.get("handler"), params.get("reason"));
        return AjaxResult.success("委托成功", batchId);
    }

    /**
     * 收回委托
     */
    @PreAuthorize("@ss.hasPermi('oa:task:coSign')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/delegate/reclaim/{taskId}")
    public AjaxResult reclaimDelegate(@PathVariable("taskId") String taskId,
                                     @RequestBody(required = false) Map<String, String> params)
    {
        oaCountersignService.reclaimDelegate(taskId, SecurityUtils.getUsername(),
                params == null ? null : params.get("reason"));
        return success();
    }

    /**
     * 提交加签/会签意见
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @Log(title = "待办任务", businessType = BusinessType.UPDATE)
    @PostMapping("/task/opinion/{itemId}")
    public AjaxResult submitOpinion(@PathVariable("itemId") Long itemId,
                                   @RequestBody Map<String, Object> params)
    {
        Object agree = params.get("agree");
        boolean approved = agree != null && Boolean.parseBoolean(String.valueOf(agree));
        String opinion = params.get("opinion") == null ? null : String.valueOf(params.get("opinion"));
        oaCountersignService.submitOpinion(itemId, SecurityUtils.getUsername(), approved, opinion);
        return success();
    }

    /**
     * 我的加签/会签待办
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/task/opinion/my")
    public TableDataInfo myOpinionTodo()
    {
        startPage();
        List<OaCountersignItem> list = oaCountersignService.listMyPending(SecurityUtils.getUsername());
        return getDataTable(list);
    }

    /**
     * 可选协同办理人（仅展示最小字段，避开 system:user:list 权限依赖）
     */
    @PreAuthorize("@ss.hasPermi('oa:task:list')")
    @GetMapping("/task/opinion/users")
    public AjaxResult handlerOptions()
    {
        return success(oaCountersignService.listHandlerOptions());
    }

    /**
     * 按流程实例查询加签/会签/委托留痕
     */
    @PreAuthorize("@ss.hasAnyPermi('oa:instance:query,oa:task:list')")
    @GetMapping("/task/opinion/list")
    public AjaxResult opinionList(@RequestParam("processInstanceId") String processInstanceId)
    {
        return success(oaCountersignService.listByProcessInstance(processInstanceId));
    }

    /**
     * 新增流程定义（BPMN XML 字符串方式）
     */
    @PreAuthorize("@ss.hasPermi('oa:definition:deploy')")
    @Log(title = "流程定义", businessType = BusinessType.INSERT)
    @PostMapping("/definition/create")
    public AjaxResult createDefinition(@RequestBody Map<String, String> params)
    {
        String processName = params.get("processName");
        String bpmnXml = params.get("bpmnXml");
        if (processName == null || bpmnXml == null)
        {
            return error("流程名称和BPMN XML不能为空");
        }
        Deployment deployment = oaWorkflowService.deployProcess(processName, bpmnXml);
        // 避免 success(String) 重载：部署ID应放入 data 而非 msg
        return AjaxResult.success("新增流程定义成功", deployment.getId());
    }

    /**
     * 从请求体中取协同办理人列表（兼容 handlers 数组与 handler 逗号分隔字符串）
     */
    @SuppressWarnings("unchecked")
    private List<String> extractHandlers(Map<String, Object> params)
    {
        List<String> handlers = new ArrayList<>();
        Object raw = params.get("handlers");
        if (raw == null)
        {
            raw = params.get("handler");
        }
        if (raw instanceof List)
        {
            for (Object item : (List<Object>) raw)
            {
                if (item != null)
                {
                    handlers.add(String.valueOf(item));
                }
            }
        }
        else if (raw != null)
        {
            for (String item : String.valueOf(raw).split(","))
            {
                if (!item.trim().isEmpty())
                {
                    handlers.add(item.trim());
                }
            }
        }
        return handlers;
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
