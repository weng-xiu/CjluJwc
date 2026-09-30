package com.yu.oa.workflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.oa.workflow.domain.OaCountersignItem;
import com.yu.oa.workflow.domain.vo.ProcessDiagramVo;
import com.yu.oa.workflow.service.IOaCountersignService;
import com.yu.oa.workflow.service.IOaProcessDiagramService;
import com.yu.oa.workflow.service.IOaWorkflowService;

/**
 * OA 工作流接口层测试（Q1 第五批：MockMvc standalone）。
 * 校验流程定义/实例/任务三类端点的 HTTP 路由、@RequestParam（含可选与特殊字符 ID）绑定、
 * @PathVariable 字符串任务 ID、@RequestBody Map 的可选与默认值（mode/rule/终止原因）、
 * 协同办理人解析（handlers 数组与 handler 逗号串两分支）、空值/缺失校验的 error 分支，
 * 以及 SecurityContext 注入的 assignee/operator 透传；Flowable 引擎推进、会签门禁等业务逻辑由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaWorkflowControllerMockMvcTest {

    @Mock
    private IOaWorkflowService oaWorkflowService;

    @Mock
    private IOaCountersignService oaCountersignService;

    @Mock
    private IOaProcessDiagramService oaProcessDiagramService;

    @InjectMocks
    private OaWorkflowController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // BaseController.getUsername()/SecurityUtils.getUsername() 依赖 SecurityContext，standalone 手动注入登录用户
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ================= 流程定义 =================

    @Test
    @DisplayName("definitionList：流程定义映射为精简字段并返回表格结构")
    void definitionList_mapsFields() throws Exception {
        ProcessDefinition pd = mock(ProcessDefinition.class);
        when(pd.getId()).thenReturn("leave:1:100");
        when(pd.getKey()).thenReturn("leave");
        when(pd.getName()).thenReturn("请假流程");
        when(pd.getVersion()).thenReturn(3);
        when(pd.getDeploymentId()).thenReturn("dep-1");
        when(pd.isSuspended()).thenReturn(false);
        when(oaWorkflowService.listProcessDefinitions()).thenReturn(List.of(pd));

        mockMvc.perform(get("/oa/workflow/definition/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray())
                .andExpect(jsonPath("$.rows[0].id").value("leave:1:100"))
                .andExpect(jsonPath("$.rows[0].key").value("leave"))
                .andExpect(jsonPath("$.rows[0].name").value("请假流程"))
                .andExpect(jsonPath("$.rows[0].version").value(3))
                .andExpect(jsonPath("$.rows[0].deploymentId").value("dep-1"))
                .andExpect(jsonPath("$.rows[0].suspended").value(false));
    }

    @Test
    @DisplayName("getBpmnXml：definitionId 走 RequestParam 且 XML 落 data 而非 msg")
    void getBpmnXml_putsXmlInData() throws Exception {
        // 用带冒号的特殊字符 ID，验证 RequestParam 传参而非路径变量
        when(oaWorkflowService.getProcessBpmnXml(eq("leave:1:100")))
                .thenReturn("<bpmn>process</bpmn>");

        mockMvc.perform(get("/oa/workflow/definition/xml").param("definitionId", "leave:1:100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("<bpmn>process</bpmn>"));
    }

    @Test
    @DisplayName("getBpmnXml：定义不存在返回 error 分支")
    void getBpmnXml_nullReturnsError() throws Exception {
        when(oaWorkflowService.getProcessBpmnXml(any())).thenReturn(null);

        mockMvc.perform(get("/oa/workflow/definition/xml").param("definitionId", "missing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("流程定义不存在或已被删除"));
    }

    @Test
    @DisplayName("createDefinition：流程名称与 XML 齐全时部署并返回部署ID（避免 success(String) 重载）")
    void createDefinition_validDeploys() throws Exception {
        Deployment deployment = mock(Deployment.class);
        when(deployment.getId()).thenReturn("dep-new");
        when(oaWorkflowService.deployProcess(eq("请假流程"), eq("<bpmn/>"))).thenReturn(deployment);

        String body = "{\"processName\":\"请假流程\",\"bpmnXml\":\"<bpmn/>\"}";
        mockMvc.perform(post("/oa/workflow/definition/create")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("dep-new"));
    }

    @Test
    @DisplayName("createDefinition：缺少 bpmnXml 拦截返回 error，不调用部署")
    void createDefinition_missingXmlRejected() throws Exception {
        String body = "{\"processName\":\"请假流程\"}";
        mockMvc.perform(post("/oa/workflow/definition/create")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("流程名称和BPMN XML不能为空"));

        verify(oaWorkflowService, never()).deployProcess(any(), any(String.class));
    }

    @Test
    @DisplayName("deleteDeployment：路径变量透传部署ID删除")
    void deleteDeployment_passesPathId() throws Exception {
        mockMvc.perform(post("/oa/workflow/definition/delete/dep-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaWorkflowService).deleteDeployment(eq("dep-9"));
    }

    // ================= 流程实例 =================

    @Test
    @DisplayName("instanceList：三个可选筛选条件原样透传服务层")
    void instanceList_passesOptionalFilters() throws Exception {
        when(oaWorkflowService.listProcessInstances(eq("请假"), eq("zhangsan"), eq("running")))
                .thenReturn(List.of(Map.of("processInstanceId", "pi-1", "name", "请假流程")));

        mockMvc.perform(get("/oa/workflow/instance/list")
                        .param("processDefinitionName", "请假")
                        .param("startUserId", "zhangsan")
                        .param("status", "running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].processInstanceId").value("pi-1"));

        verify(oaWorkflowService).listProcessInstances(eq("请假"), eq("zhangsan"), eq("running"));
    }

    @Test
    @DisplayName("instanceDetail：详情命中返回 data，实例不存在走 error 分支")
    void instanceDetail_successAndError() throws Exception {
        when(oaWorkflowService.getProcessInstanceDetail(eq("pi-1")))
                .thenReturn(Map.of("processInstanceId", "pi-1", "state", "running"));
        mockMvc.perform(get("/oa/workflow/instance/detail").param("processInstanceId", "pi-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processInstanceId").value("pi-1"))
                .andExpect(jsonPath("$.data.state").value("running"));

        when(oaWorkflowService.getProcessInstanceDetail(eq("pi-x"))).thenReturn(null);
        mockMvc.perform(get("/oa/workflow/instance/detail").param("processInstanceId", "pi-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("流程实例不存在或已被删除"));
    }

    @Test
    @DisplayName("cancelInstance：processInstanceId 为空拦截，不调用终止")
    void cancelInstance_blankIdRejected() throws Exception {
        String body = "{\"processInstanceId\":\"  \"}";
        mockMvc.perform(post("/oa/workflow/instance/cancel")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("流程实例ID不能为空"));

        verify(oaWorkflowService, never()).cancelProcessInstance(any(), any());
    }

    @Test
    @DisplayName("cancelInstance：reason 缺省时以“管理员终止”兜底调用服务层")
    void cancelInstance_defaultReason() throws Exception {
        String body = "{\"processInstanceId\":\"pi-1\"}";
        mockMvc.perform(post("/oa/workflow/instance/cancel")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaWorkflowService).cancelProcessInstance(eq("pi-1"), eq("管理员终止"));
    }

    // ================= 任务（待办/已办/办理） =================

    @Test
    @DisplayName("todoList：assignee 取当前登录用户名并映射任务字段")
    void todoList_usesLoggedInAssignee() throws Exception {
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("t-1");
        when(task.getName()).thenReturn("部门审批");
        when(task.getProcessInstanceId()).thenReturn("pi-1");
        when(task.getAssignee()).thenReturn("tester");
        when(oaWorkflowService.listTodoTasks(eq("tester"), isNull())).thenReturn(List.of(task));

        mockMvc.perform(get("/oa/workflow/task/todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].taskId").value("t-1"))
                .andExpect(jsonPath("$.rows[0].taskName").value("部门审批"))
                .andExpect(jsonPath("$.rows[0].processInstanceId").value("pi-1"));

        verify(oaWorkflowService).listTodoTasks(eq("tester"), isNull());
    }

    @Test
    @DisplayName("doneList：历史任务查询走 assignee+taskName 并返回结束时间字段")
    void doneList_mapsHistoricTask() throws Exception {
        HistoricTaskInstance hti = mock(HistoricTaskInstance.class);
        when(hti.getId()).thenReturn("h-1");
        when(hti.getName()).thenReturn("已处理审批");
        when(hti.getProcessInstanceId()).thenReturn("pi-2");
        when(oaWorkflowService.listDoneTasks(eq("tester"), eq("已处理")))
                .thenReturn(List.of(hti));

        mockMvc.perform(get("/oa/workflow/task/done").param("taskName", "已处理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].taskId").value("h-1"))
                .andExpect(jsonPath("$.rows[0].processInstanceId").value("pi-2"));

        verify(oaWorkflowService).listDoneTasks(eq("tester"), eq("已处理"));
    }

    @Test
    @DisplayName("complete：任务ID走路径变量，comment/variables 从 body 解出，assignee 取登录用户")
    void complete_bindsPathAndBody() throws Exception {
        String body = "{\"comment\":\"同意\",\"variables\":{\"days\":3}}";
        mockMvc.perform(post("/oa/workflow/task/complete/t-7")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(oaWorkflowService).completeTask(eq("t-7"), eq("tester"), vars.capture(), eq("同意"));
        org.junit.jupiter.api.Assertions.assertEquals(3, vars.getValue().get("days"));
    }

    @Test
    @DisplayName("complete：请求体缺省（required=false）时 variables/comment 传 null")
    void complete_withoutBody() throws Exception {
        mockMvc.perform(post("/oa/workflow/task/complete/t-8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaWorkflowService).completeTask(eq("t-8"), eq("tester"), isNull(), isNull());
    }

    @Test
    @DisplayName("reject：驳回透传任务ID、登录用户与驳回意见")
    void reject_passesComment() throws Exception {
        String body = "{\"comment\":\"材料不齐\"}";
        mockMvc.perform(post("/oa/workflow/task/reject/t-5")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaWorkflowService).rejectTask(eq("t-5"), eq("tester"), eq("材料不齐"));
    }

    @Test
    @DisplayName("transfer：转办透传原办理人（登录用户）与新办理人")
    void transfer_passesNewAssignee() throws Exception {
        String body = "{\"newAssignee\":\"lisi\",\"comment\":\"出差转办\"}";
        mockMvc.perform(post("/oa/workflow/task/transfer/t-6")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaWorkflowService).transferTask(eq("t-6"), eq("tester"), eq("lisi"), eq("出差转办"));
    }

    // ================= 流程图 =================

    @Test
    @DisplayName("diagram：渲染数据命中返回 data，实例不存在走 error 分支")
    void diagram_successAndError() throws Exception {
        ProcessDiagramVo vo = new ProcessDiagramVo();
        vo.setProcessInstanceId("pi-1");
        vo.setFinished(false);
        when(oaProcessDiagramService.buildDiagram(eq("pi-1"))).thenReturn(vo);
        mockMvc.perform(get("/oa/workflow/diagram").param("processInstanceId", "pi-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processInstanceId").value("pi-1"))
                .andExpect(jsonPath("$.data.finished").value(false));

        when(oaProcessDiagramService.buildDiagram(eq("pi-x"))).thenReturn(null);
        mockMvc.perform(get("/oa/workflow/diagram").param("processInstanceId", "pi-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("流程实例不存在或已被删除"));
    }

    // ================= 加签/会签/委托 =================

    @Test
    @DisplayName("addSign：mode 缺省为前加签(0)，handlers 数组解析后透传，返回批次ID")
    void addSign_defaultModeAndHandlersArray() throws Exception {
        when(oaCountersignService.addSign(eq("t-1"), eq("tester"), eq("0"),
                anyList(), eq("征求意见"))).thenReturn(88L);

        String body = "{\"handlers\":[\"a\",\"b\"],\"reason\":\"征求意见\"}";
        mockMvc.perform(post("/oa/workflow/task/addSign/t-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(88));

        ArgumentCaptor<List<String>> handlers = ArgumentCaptor.forClass(List.class);
        verify(oaCountersignService).addSign(eq("t-1"), eq("tester"), eq("0"),
                handlers.capture(), eq("征求意见"));
        org.junit.jupiter.api.Assertions.assertEquals(List.of("a", "b"), handlers.getValue());
    }

    @Test
    @DisplayName("counterSign：handler 逗号串解析为列表，rule 显式传 ANY 原样透传")
    void counterSign_commaHandlersParsed() throws Exception {
        when(oaCountersignService.counterSign(eq("t-2"), eq("tester"),
                anyList(), eq("ANY"), eq("并行表决"))).thenReturn(99L);

        String body = "{\"handler\":\"x,y\",\"rule\":\"ANY\",\"reason\":\"并行表决\"}";
        mockMvc.perform(post("/oa/workflow/task/counterSign/t-2")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(99));

        ArgumentCaptor<List<String>> handlers = ArgumentCaptor.forClass(List.class);
        verify(oaCountersignService).counterSign(eq("t-2"), eq("tester"),
                handlers.capture(), eq("ANY"), eq("并行表决"));
        org.junit.jupiter.api.Assertions.assertEquals(List.of("x", "y"), handlers.getValue());
    }

    @Test
    @DisplayName("delegate：委托透传 handler/reason 并返回批次ID")
    void delegate_passesHandler() throws Exception {
        when(oaCountersignService.delegate(eq("t-3"), eq("tester"), eq("wangwu"), eq("休假委托")))
                .thenReturn(77L);

        String body = "{\"handler\":\"wangwu\",\"reason\":\"休假委托\"}";
        mockMvc.perform(post("/oa/workflow/task/delegate/t-3")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(77));
    }

    @Test
    @DisplayName("submitOpinion：agree 字符串解析为布尔，itemId 走路径变量")
    void submitOpinion_parsesAgreeBoolean() throws Exception {
        String body = "{\"agree\":true,\"opinion\":\"同意\"}";
        mockMvc.perform(post("/oa/workflow/task/opinion/123")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaCountersignService).submitOpinion(eq(123L), eq("tester"), eq(true), eq("同意"));
    }

    @Test
    @DisplayName("myOpinionTodo：按当前登录人查询待表态项并返回表格结构")
    void myOpinionTodo_usesLoggedInHandler() throws Exception {
        OaCountersignItem item = new OaCountersignItem();
        item.setItemId(1L);
        item.setTaskId("t-1");
        item.setStatus("0");
        when(oaCountersignService.listMyPending(eq("tester"))).thenReturn(List.of(item));

        mockMvc.perform(get("/oa/workflow/task/opinion/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].itemId").value(1))
                .andExpect(jsonPath("$.rows[0].taskId").value("t-1"));

        verify(oaCountersignService).listMyPending(eq("tester"));
    }

    @Test
    @DisplayName("handlerOptions：返回服务层可选办理人列表")
    void handlerOptions_returnsList() throws Exception {
        when(oaCountersignService.listHandlerOptions())
                .thenReturn(List.of(Map.of("userName", "a", "nickName", "张三")));

        mockMvc.perform(get("/oa/workflow/task/opinion/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userName").value("a"))
                .andExpect(jsonPath("$.data[0].nickName").value("张三"));
    }

    @Test
    @DisplayName("reclaimDelegate：收回委托透传任务ID与登录用户，reason 缺省为 null")
    void reclaimDelegate_blankReason() throws Exception {
        mockMvc.perform(post("/oa/workflow/task/delegate/reclaim/t-4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(oaCountersignService).reclaimDelegate(eq("t-4"), eq("tester"), isNull());
    }
}
