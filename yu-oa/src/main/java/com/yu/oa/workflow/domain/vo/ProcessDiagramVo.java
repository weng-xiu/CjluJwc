package com.yu.oa.workflow.domain.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程图渲染数据（节点 + 连线 + 进度状态 + 服务端自动布局坐标）
 *
 * 说明：项目内流程定义均为手写 BPMN，不含 BPMNDI 图形信息，且前端未引入 bpmn-js
 * 等图形库依赖，故在此按 BPMN 拓扑做分层自动布局，输出纯坐标数据由前端 SVG 渲染，
 * 零新增依赖即可实现流程图可视化与当前节点高亮。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class ProcessDiagramVo
{
    /** 流程实例ID */
    private String processInstanceId;

    /** 流程定义ID */
    private String processDefinitionId;

    /** 流程名称 */
    private String processDefinitionName;

    /** 流程是否已结束 */
    private boolean finished;

    /** 节点集合 */
    private List<Node> nodes = new ArrayList<>();

    /** 连线集合 */
    private List<Edge> edges = new ArrayList<>();

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId()
    {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId)
    {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionName()
    {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName)
    {
        this.processDefinitionName = processDefinitionName;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public void setFinished(boolean finished)
    {
        this.finished = finished;
    }

    public List<Node> getNodes()
    {
        return nodes;
    }

    public void setNodes(List<Node> nodes)
    {
        this.nodes = nodes;
    }

    public List<Edge> getEdges()
    {
        return edges;
    }

    public void setEdges(List<Edge> edges)
    {
        this.edges = edges;
    }

    /**
     * 流程节点
     */
    public static class Node
    {
        /** 节点ID（BPMN element id） */
        private String id;

        /** 节点名称 */
        private String name;

        /** 节点类型：startEvent/endEvent/userTask/serviceTask/gateway/other */
        private String type;

        /** 布局坐标（左上角） */
        private int x;

        private int y;

        /** 节点尺寸 */
        private int width;

        private int height;

        /** 进度状态：finished 已办理 active 进行中 pending 未到达 */
        private String state;

        /** 当前/最后办理人（用户任务） */
        private String assignee;

        /** 办理时间 */
        private String finishTime;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getType()
        {
            return type;
        }

        public void setType(String type)
        {
            this.type = type;
        }

        public int getX()
        {
            return x;
        }

        public void setX(int x)
        {
            this.x = x;
        }

        public int getY()
        {
            return y;
        }

        public void setY(int y)
        {
            this.y = y;
        }

        public int getWidth()
        {
            return width;
        }

        public void setWidth(int width)
        {
            this.width = width;
        }

        public int getHeight()
        {
            return height;
        }

        public void setHeight(int height)
        {
            this.height = height;
        }

        public String getState()
        {
            return state;
        }

        public void setState(String state)
        {
            this.state = state;
        }

        public String getAssignee()
        {
            return assignee;
        }

        public void setAssignee(String assignee)
        {
            this.assignee = assignee;
        }

        public String getFinishTime()
        {
            return finishTime;
        }

        public void setFinishTime(String finishTime)
        {
            this.finishTime = finishTime;
        }
    }

    /**
     * 流程连线
     */
    public static class Edge
    {
        /** 连线ID */
        private String id;

        /** 连线名称 */
        private String name;

        /** 源节点ID */
        private String source;

        /** 目标节点ID */
        private String target;

        /** 是否带条件表达式 */
        private boolean conditional;

        /** 进度状态：passed 已流转 pending 未流转 */
        private String state;

        /** 折线路径点（含起终点） */
        private List<int[]> points = new ArrayList<>();

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getSource()
        {
            return source;
        }

        public void setSource(String source)
        {
            this.source = source;
        }

        public String getTarget()
        {
            return target;
        }

        public void setTarget(String target)
        {
            this.target = target;
        }

        public boolean isConditional()
        {
            return conditional;
        }

        public void setConditional(boolean conditional)
        {
            this.conditional = conditional;
        }

        public String getState()
        {
            return state;
        }

        public void setState(String state)
        {
            this.state = state;
        }

        public List<int[]> getPoints()
        {
            return points;
        }

        public void setPoints(List<int[]> points)
        {
            this.points = points;
        }
    }
}
