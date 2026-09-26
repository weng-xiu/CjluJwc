package com.yu.oa.workflow.service.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.oa.workflow.domain.vo.ProcessDiagramVo;
import com.yu.oa.workflow.service.IOaProcessDiagramService;

/**
 * 流程图渲染数据服务实现
 *
 * 项目内流程定义均为手写 BPMN（无 BPMNDI 图形信息），故按拓扑做分层自动布局：
 * 以开始事件为第 0 层，沿连线做最长路径松弛定层，同层按出现顺序排布并垂直居中，
 * 回退连线（驳回环）走节点下方绕行，最终输出纯坐标 + 状态数据由前端 SVG 渲染。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@Service
public class OaProcessDiagramServiceImpl implements IOaProcessDiagramService
{
    /** 列间距 */
    private static final int COL_GAP = 230;

    /** 行间距 */
    private static final int ROW_GAP = 120;

    /** 画布左边距 */
    private static final int MARGIN_X = 40;

    /** 画布上边距 */
    private static final int MARGIN_Y = 40;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Override
    public ProcessDiagramVo buildDiagram(String processInstanceId)
    {
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hpi == null)
        {
            return null;
        }
        BpmnModel model = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
        Process process = model == null ? null : model.getMainProcess();
        if (process == null)
        {
            return null;
        }

        ProcessDiagramVo vo = new ProcessDiagramVo();
        vo.setProcessInstanceId(processInstanceId);
        vo.setProcessDefinitionId(hpi.getProcessDefinitionId());
        vo.setProcessDefinitionName(hpi.getProcessDefinitionName());
        vo.setFinished(hpi.getEndTime() != null);

        Map<String, ProcessDiagramVo.Node> nodeMap = collectNodes(process);
        List<ProcessDiagramVo.Edge> edges = collectEdges(process, nodeMap);
        if (nodeMap.isEmpty())
        {
            return vo;
        }

        Map<String, Integer> layers = assignLayers(nodeMap, edges);
        layout(nodeMap, edges, layers);

        fillProgress(vo, nodeMap, edges, processInstanceId);

        vo.setNodes(new ArrayList<>(nodeMap.values()));
        vo.setEdges(edges);
        return vo;
    }

    /**
     * 收集节点（跳过连线；子流程整体作为一个节点呈现）
     */
    private Map<String, ProcessDiagramVo.Node> collectNodes(Process process)
    {
        Map<String, ProcessDiagramVo.Node> nodeMap = new LinkedHashMap<>();
        for (FlowElement element : process.getFlowElements())
        {
            if (element instanceof SequenceFlow || element == null || element.getId() == null)
            {
                continue;
            }
            ProcessDiagramVo.Node node = new ProcessDiagramVo.Node();
            node.setId(element.getId());
            node.setName(resolveName(element));
            node.setType(resolveType(element));
            int[] size = resolveSize(node.getType());
            node.setWidth(size[0]);
            node.setHeight(size[1]);
            node.setState("pending");
            nodeMap.put(element.getId(), node);
        }
        return nodeMap;
    }

    /**
     * 收集连线，仅保留两端都在本流程节点集内的连线
     */
    private List<ProcessDiagramVo.Edge> collectEdges(Process process, Map<String, ProcessDiagramVo.Node> nodeMap)
    {
        List<ProcessDiagramVo.Edge> edges = new ArrayList<>();
        for (FlowElement element : process.getFlowElements())
        {
            if (!(element instanceof SequenceFlow))
            {
                continue;
            }
            SequenceFlow flow = (SequenceFlow) element;
            if (!nodeMap.containsKey(flow.getSourceRef()) || !nodeMap.containsKey(flow.getTargetRef()))
            {
                continue;
            }
            ProcessDiagramVo.Edge edge = new ProcessDiagramVo.Edge();
            edge.setId(flow.getId());
            edge.setName(flow.getName());
            edge.setSource(flow.getSourceRef());
            edge.setTarget(flow.getTargetRef());
            edge.setConditional(flow.getConditionExpression() != null
                    && !flow.getConditionExpression().trim().isEmpty());
            edge.setState("pending");
            edges.add(edge);
        }
        return edges;
    }

    /**
     * 分层：先以 DFS 识别回退边（驳回环），再在非回退边上做最长路径松弛
     */
    private Map<String, Integer> assignLayers(Map<String, ProcessDiagramVo.Node> nodeMap,
                                              List<ProcessDiagramVo.Edge> edges)
    {
        Map<String, Integer> layer = new HashMap<>();
        for (String id : nodeMap.keySet())
        {
            layer.put(id, 0);
        }
        Set<String> backEdgeIds = detectBackEdges(nodeMap, edges);
        int iterations = nodeMap.size() + 1;
        for (int i = 0; i < iterations; i++)
        {
            boolean changed = false;
            for (ProcessDiagramVo.Edge edge : edges)
            {
                if (backEdgeIds.contains(edge.getId()))
                {
                    continue;
                }
                int source = layer.get(edge.getSource());
                int target = layer.get(edge.getTarget());
                if (target < source + 1)
                {
                    layer.put(edge.getTarget(), source + 1);
                    changed = true;
                }
            }
            if (!changed)
            {
                break;
            }
        }
        return layer;
    }

    /**
     * 以三色标记 DFS 识别回退边：指向仍在递归栈上节点的连线视为回退
     */
    private Set<String> detectBackEdges(Map<String, ProcessDiagramVo.Node> nodeMap,
                                        List<ProcessDiagramVo.Edge> edges)
    {
        Map<String, List<ProcessDiagramVo.Edge>> outgoing = new HashMap<>();
        for (ProcessDiagramVo.Edge edge : edges)
        {
            outgoing.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        }
        Set<String> back = new HashSet<>();
        Map<String, Integer> color = new HashMap<>();
        List<String> roots = new ArrayList<>();
        for (ProcessDiagramVo.Node node : nodeMap.values())
        {
            if ("startEvent".equals(node.getType()))
            {
                roots.add(node.getId());
            }
        }
        for (String id : nodeMap.keySet())
        {
            if (!roots.contains(id))
            {
                roots.add(id);
            }
        }
        for (String root : roots)
        {
            if (color.getOrDefault(root, 0) == 0)
            {
                visit(root, outgoing, color, back);
            }
        }
        return back;
    }

    /**
     * DFS 三色标记（0 未访问 / 1 在栈上 / 2 已完成），显式栈避免深层递归
     */
    private void visit(String root, Map<String, List<ProcessDiagramVo.Edge>> outgoing,
                       Map<String, Integer> color, Set<String> back)
    {
        Deque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[] { root, 0 });
        color.put(root, 1);
        while (!stack.isEmpty())
        {
            Object[] frame = stack.peek();
            String current = (String) frame[0];
            int index = (Integer) frame[1];
            List<ProcessDiagramVo.Edge> flows = outgoing.getOrDefault(current, Collections.emptyList());
            if (index < flows.size())
            {
                frame[1] = index + 1;
                ProcessDiagramVo.Edge edge = flows.get(index);
                String next = edge.getTarget();
                int nextColor = color.getOrDefault(next, 0);
                if (nextColor == 1)
                {
                    back.add(edge.getId());
                }
                else if (nextColor == 0)
                {
                    color.put(next, 1);
                    stack.push(new Object[] { next, 0 });
                }
            }
            else
            {
                stack.pop();
                color.put(current, 2);
            }
        }
    }

    /**
     * 计算坐标：同层垂直居中，连线生成折点
     */
    private void layout(Map<String, ProcessDiagramVo.Node> nodeMap, List<ProcessDiagramVo.Edge> edges,
                        Map<String, Integer> layers)
    {
        Map<Integer, List<ProcessDiagramVo.Node>> byLayer = new LinkedHashMap<>();
        for (ProcessDiagramVo.Node node : nodeMap.values())
        {
            int layer = layers.get(node.getId());
            byLayer.computeIfAbsent(layer, k -> new ArrayList<>()).add(node);
        }
        // 按前驱节点的行序重排同层节点，减少连线交叉
        Map<String, Integer> rowIndex = new HashMap<>();
        Map<Integer, List<ProcessDiagramVo.Node>> ordered = new LinkedHashMap<>();
        List<Integer> layerKeys = new ArrayList<>(byLayer.keySet());
        layerKeys.sort(Integer::compareTo);
        for (Integer layer : layerKeys)
        {
            List<ProcessDiagramVo.Node> nodes = byLayer.get(layer);
            Map<ProcessDiagramVo.Node, Double> sortKey = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++)
            {
                ProcessDiagramVo.Node node = nodes.get(i);
                List<Integer> parents = new ArrayList<>();
                for (ProcessDiagramVo.Edge edge : edges)
                {
                    if (edge.getTarget().equals(node.getId()) && rowIndex.containsKey(edge.getSource()))
                    {
                        parents.add(rowIndex.get(edge.getSource()));
                    }
                }
                double key = parents.isEmpty() ? i : parents.stream().mapToInt(Integer::intValue).average().orElse(i);
                sortKey.put(node, key);
            }
            nodes.sort((a, b) -> Double.compare(sortKey.get(a), sortKey.get(b)));
            for (int i = 0; i < nodes.size(); i++)
            {
                rowIndex.put(nodes.get(i).getId(), i);
            }
            ordered.put(layer, nodes);
        }
        int maxRows = 1;
        for (List<ProcessDiagramVo.Node> nodes : ordered.values())
        {
            maxRows = Math.max(maxRows, nodes.size());
        }
        for (Map.Entry<Integer, List<ProcessDiagramVo.Node>> entry : ordered.entrySet())
        {
            List<ProcessDiagramVo.Node> nodes = entry.getValue();
            int offset = (maxRows - nodes.size()) * ROW_GAP / 2;
            for (int i = 0; i < nodes.size(); i++)
            {
                ProcessDiagramVo.Node node = nodes.get(i);
                node.setX(MARGIN_X + entry.getKey() * COL_GAP);
                node.setY(MARGIN_Y + offset + i * ROW_GAP);
            }
        }
        routeEdges(nodeMap, edges, layers, ordered);
    }

    /**
     * 连线走线：前向走直角折线，回退边走节点下方绕行
     */
    private void routeEdges(Map<String, ProcessDiagramVo.Node> nodeMap, List<ProcessDiagramVo.Edge> edges,
                            Map<String, Integer> layers, Map<Integer, List<ProcessDiagramVo.Node>> ordered)
    {
        int maxY = 0;
        for (ProcessDiagramVo.Node node : nodeMap.values())
        {
            maxY = Math.max(maxY, node.getY() + node.getHeight());
        }
        int lane = 0;
        for (ProcessDiagramVo.Edge edge : edges)
        {
            ProcessDiagramVo.Node source = nodeMap.get(edge.getSource());
            ProcessDiagramVo.Node target = nodeMap.get(edge.getTarget());
            List<int[]> points = new ArrayList<>();
            boolean forward = layers.get(target.getId()) > layers.get(source.getId());
            if (forward)
            {
                int x1 = source.getX() + source.getWidth();
                int y1 = source.getY() + source.getHeight() / 2;
                int x2 = target.getX();
                int y2 = target.getY() + target.getHeight() / 2;
                if (y1 == y2)
                {
                    points.add(new int[] { x1, y1 });
                    points.add(new int[] { x2, y2 });
                }
                else
                {
                    int midX = x1 + Math.max(24, (x2 - x1) / 2);
                    points.add(new int[] { x1, y1 });
                    points.add(new int[] { midX, y1 });
                    points.add(new int[] { midX, y2 });
                    points.add(new int[] { x2, y2 });
                }
            }
            else
            {
                // 回退连线：从源节点底部出发，走画布下方通道回到目标节点底部
                int laneY = maxY + 40 + lane * 26;
                lane++;
                int x1 = source.getX() + source.getWidth() / 2;
                int x2 = target.getX() + target.getWidth() / 2;
                points.add(new int[] { x1, source.getY() + source.getHeight() });
                points.add(new int[] { x1, laneY });
                points.add(new int[] { x2, laneY });
                points.add(new int[] { x2, target.getY() + target.getHeight() });
            }
            edge.setPoints(points);
        }
    }

    /**
     * 填充进度：已办节点、进行中节点、已流转连线
     */
    private void fillProgress(ProcessDiagramVo vo, Map<String, ProcessDiagramVo.Node> nodeMap,
                              List<ProcessDiagramVo.Edge> edges, String processInstanceId)
    {
        Set<String> finishedActivities = new HashSet<>();
        Set<String> activeActivities = new HashSet<>();
        Set<String> passedFlows = new HashSet<>();
        for (HistoricActivityInstance hai : historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).list())
        {
            if ("sequenceFlow".equals(hai.getActivityType()))
            {
                if (hai.getEndTime() != null || hai.getStartTime() != null)
                {
                    passedFlows.add(hai.getActivityId());
                }
                continue;
            }
            if (hai.getEndTime() != null)
            {
                finishedActivities.add(hai.getActivityId());
            }
            else
            {
                activeActivities.add(hai.getActivityId());
            }
        }
        Map<String, HistoricTaskInstance> taskByNode = new HashMap<>();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc().list();
        for (HistoricTaskInstance task : tasks)
        {
            // 同一节点可能被驳回重入，保留最后一次办理信息
            taskByNode.put(task.getTaskDefinitionKey(), task);
        }
        Map<String, Task> running = new HashMap<>();
        for (Task task : taskService.createTaskQuery().processInstanceId(processInstanceId).list())
        {
            running.put(task.getTaskDefinitionKey(), task);
        }
        for (ProcessDiagramVo.Node node : nodeMap.values())
        {
            HistoricTaskInstance task = taskByNode.get(node.getId());
            if (task != null)
            {
                node.setAssignee(task.getAssignee());
                if (task.getEndTime() != null)
                {
                    node.setFinishTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, task.getEndTime()));
                }
            }
            if (running.containsKey(node.getId()) || activeActivities.contains(node.getId()))
            {
                node.setState("active");
                Task task2 = running.get(node.getId());
                if (task2 != null && task2.getAssignee() != null)
                {
                    node.setAssignee(task2.getAssignee());
                }
            }
            else if (finishedActivities.contains(node.getId()))
            {
                node.setState("finished");
            }
        }
        for (ProcessDiagramVo.Edge edge : edges)
        {
            boolean passed = passedFlows.contains(edge.getId())
                    || ("finished".equals(nodeMap.get(edge.getSource()).getState())
                        && !"pending".equals(nodeMap.get(edge.getTarget()).getState()));
            edge.setState(passed ? "passed" : "pending");
        }
    }

    private String resolveName(FlowElement element)
    {
        if (element.getName() != null && !element.getName().trim().isEmpty())
        {
            return element.getName();
        }
        if (element instanceof StartEvent)
        {
            return "开始";
        }
        if (element instanceof EndEvent)
        {
            return "结束";
        }
        return element.getId();
    }

    private String resolveType(FlowElement element)
    {
        if (element instanceof StartEvent)
        {
            return "startEvent";
        }
        if (element instanceof EndEvent)
        {
            return "endEvent";
        }
        if (element instanceof Gateway)
        {
            return "gateway";
        }
        if (element instanceof SubProcess)
        {
            return "subProcess";
        }
        if (element instanceof UserTask)
        {
            return "userTask";
        }
        return "task";
    }

    private int[] resolveSize(String type)
    {
        if ("startEvent".equals(type) || "endEvent".equals(type))
        {
            return new int[] { 46, 46 };
        }
        if ("gateway".equals(type))
        {
            return new int[] { 58, 58 };
        }
        if ("subProcess".equals(type))
        {
            return new int[] { 160, 70 };
        }
        return new int[] { 150, 64 };
    }
}
