package com.yu.oa.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 业务流程实例关联对象 oa_process_instance
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaProcessInstance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 实例ID */
    private Long instanceId;

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务主键 */
    @Excel(name = "业务主键")
    private Long businessId;

    /** 流程定义ID */
    @Excel(name = "流程定义ID")
    private Long definitionId;

    /** Flowable 流程实例ID */
    @Excel(name = "流程实例ID")
    private String procInstId;

    /** 发起人用户ID */
    @Excel(name = "发起人用户ID")
    private Long starterId;

    /** 发起人姓名 */
    @Excel(name = "发起人姓名")
    private String starterName;

    /** 当前任务名称 */
    @Excel(name = "当前任务名称")
    private String currentTaskName;

    /** 当前处理人 */
    @Excel(name = "当前处理人")
    private String currentAssignee;

    /** 流程状态（0运行中 1已完成 2已驳回 3已撤销） */
    @Excel(name = "流程状态", readConverterExp = "0=运行中,1=已完成,2=已驳回,3=已撤销")
    private String processStatus;

    /** 发起时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发起时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public Long getInstanceId() { return instanceId; }
    public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }

    public String getProcInstId() { return procInstId; }
    public void setProcInstId(String procInstId) { this.procInstId = procInstId; }

    public Long getStarterId() { return starterId; }
    public void setStarterId(Long starterId) { this.starterId = starterId; }

    public String getStarterName() { return starterName; }
    public void setStarterName(String starterName) { this.starterName = starterName; }

    public String getCurrentTaskName() { return currentTaskName; }
    public void setCurrentTaskName(String currentTaskName) { this.currentTaskName = currentTaskName; }

    public String getCurrentAssignee() { return currentAssignee; }
    public void setCurrentAssignee(String currentAssignee) { this.currentAssignee = currentAssignee; }

    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("instanceId", getInstanceId())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("definitionId", getDefinitionId())
            .append("procInstId", getProcInstId())
            .append("starterId", getStarterId())
            .append("starterName", getStarterName())
            .append("currentTaskName", getCurrentTaskName())
            .append("currentAssignee", getCurrentAssignee())
            .append("processStatus", getProcessStatus())
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
