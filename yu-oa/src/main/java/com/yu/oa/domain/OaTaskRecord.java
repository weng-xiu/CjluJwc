package com.yu.oa.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 审批任务记录对象 oa_task_record
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaTaskRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 流程实例ID */
    @Excel(name = "流程实例ID")
    private Long instanceId;

    /** Flowable 任务ID */
    @Excel(name = "任务ID")
    private String taskId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 处理人用户ID */
    @Excel(name = "处理人用户ID")
    private Long assigneeId;

    /** 处理人姓名 */
    @Excel(name = "处理人姓名")
    private String assigneeName;

    /** 操作类型（0通过 1驳回 2转办 3会签 4撤销） */
    @Excel(name = "操作类型", readConverterExp = "0=通过,1=驳回,2=转办,3=会签,4=撤销")
    private String actionType;

    /** 审批意见 */
    @Excel(name = "审批意见")
    private String comment;

    /** 任务开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "任务开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 任务结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "任务结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("instanceId", getInstanceId())
            .append("taskId", getTaskId())
            .append("taskName", getTaskName())
            .append("assigneeId", getAssigneeId())
            .append("assigneeName", getAssigneeName())
            .append("actionType", getActionType())
            .append("comment", getComment())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
