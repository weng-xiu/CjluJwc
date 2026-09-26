package com.yu.oa.workflow.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 加签/会签/委托明细对象 oa_countersign_item
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class OaCountersignItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    private Long itemId;

    /** 批次ID */
    private Long batchId;

    /** Flowable 任务ID（冗余便于按任务检索） */
    private String taskId;

    /** Flowable 流程实例ID（冗余便于按实例检索） */
    private String processInstanceId;

    /** 被加签/会签/委托的办理人（登录名） */
    private String handler;

    /** 办理人昵称 */
    private String handlerName;

    /** 表决结果：0未表态 1同意 2不同意 */
    private String vote;

    /** 意见内容 */
    private String opinion;

    /** 明细状态：0待处理 1已处理 2已取消 */
    private String status;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;

    /** 所属批次动作类型（联表展示字段） */
    private String batchMode;

    /** 所属批次节点名称（联表展示字段） */
    private String nodeName;

    /** 所属批次流程名称（联表展示字段） */
    private String processDefinitionName;

    /** 所属批次状态（联表展示字段） */
    private String batchStatus;

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getBatchId()
    {
        return batchId;
    }

    public void setBatchId(Long batchId)
    {
        this.batchId = batchId;
    }

    public String getTaskId()
    {
        return taskId;
    }

    public void setTaskId(String taskId)
    {
        this.taskId = taskId;
    }

    public String getProcessInstanceId()
    {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId)
    {
        this.processInstanceId = processInstanceId;
    }

    public String getHandler()
    {
        return handler;
    }

    public void setHandler(String handler)
    {
        this.handler = handler;
    }

    public String getHandlerName()
    {
        return handlerName;
    }

    public void setHandlerName(String handlerName)
    {
        this.handlerName = handlerName;
    }

    public String getVote()
    {
        return vote;
    }

    public void setVote(String vote)
    {
        this.vote = vote;
    }

    public String getOpinion()
    {
        return opinion;
    }

    public void setOpinion(String opinion)
    {
        this.opinion = opinion;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getHandleTime()
    {
        return handleTime;
    }

    public void setHandleTime(Date handleTime)
    {
        this.handleTime = handleTime;
    }

    public String getBatchMode()
    {
        return batchMode;
    }

    public void setBatchMode(String batchMode)
    {
        this.batchMode = batchMode;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }

    public String getProcessDefinitionName()
    {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName)
    {
        this.processDefinitionName = processDefinitionName;
    }

    public String getBatchStatus()
    {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus)
    {
        this.batchStatus = batchStatus;
    }
}
