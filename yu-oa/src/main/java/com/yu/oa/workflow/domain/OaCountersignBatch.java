package com.yu.oa.workflow.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 加签/会签/委托批次对象 oa_countersign_batch
 *
 * 统一承载 O1 通用工作流增强的四类协同动作：
 * mode 0 前加签（意见未齐不可提交）、1 后加签（本人已表态，等待加签人意见后释放流程）、
 * 2 会签（多人并行表决，按规则释放流程）、3 委托（他人代办，办结后知会原办理人）。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class OaCountersignBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** Flowable 任务ID */
    private String taskId;

    /** Flowable 流程实例ID */
    private String processInstanceId;

    /** 流程定义名称（冗余，便于列表展示） */
    private String processDefinitionName;

    /** 节点ID */
    private String nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 动作类型：0前加签 1后加签 2会签 3委托 */
    private String mode;

    /** 表决规则：ALL 全部同意通过 ANY 一人同意即决（仅会签使用） */
    private String rule;

    /** 发起人（登录名） */
    private String initiator;

    /** 应处理人数 */
    private Integer totalCount;

    /** 已处理人数 */
    private Integer doneCount;

    /** 同意人数 */
    private Integer agreeCount;

    /** 不同意人数 */
    private Integer disagreeCount;

    /** 批次状态：0进行中 1意见已齐 2已取消 */
    private String status;

    /** 意见齐备（门禁释放）时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date releaseTime;

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

    public String getProcessDefinitionName()
    {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName)
    {
        this.processDefinitionName = processDefinitionName;
    }

    public String getNodeId()
    {
        return nodeId;
    }

    public void setNodeId(String nodeId)
    {
        this.nodeId = nodeId;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public void setNodeName(String nodeName)
    {
        this.nodeName = nodeName;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String mode)
    {
        this.mode = mode;
    }

    public String getRule()
    {
        return rule;
    }

    public void setRule(String rule)
    {
        this.rule = rule;
    }

    public String getInitiator()
    {
        return initiator;
    }

    public void setInitiator(String initiator)
    {
        this.initiator = initiator;
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getDoneCount()
    {
        return doneCount;
    }

    public void setDoneCount(Integer doneCount)
    {
        this.doneCount = doneCount;
    }

    public Integer getAgreeCount()
    {
        return agreeCount;
    }

    public void setAgreeCount(Integer agreeCount)
    {
        this.agreeCount = agreeCount;
    }

    public Integer getDisagreeCount()
    {
        return disagreeCount;
    }

    public void setDisagreeCount(Integer disagreeCount)
    {
        this.disagreeCount = disagreeCount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getReleaseTime()
    {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime)
    {
        this.releaseTime = releaseTime;
    }
}
