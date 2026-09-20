package com.yu.dis.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 数据同步任务对象 dis_sync_task
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public class DisSyncTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务ID */
    private Long taskId;

    /** 任务名称 */
    @Excel(name = "任务名称")
    private String taskName;

    /** 任务编码 */
    @Excel(name = "任务编码")
    private String taskCode;

    /** 外部系统ID */
    @Excel(name = "外部系统ID")
    private Long systemId;

    /** 接口ID */
    @Excel(name = "接口ID")
    private Long interfaceId;

    /** Cron表达式 */
    @Excel(name = "Cron表达式")
    private String cronExpression;

    /** 上次执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "上次执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastExecuteTime;

    /** 下次执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "下次执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date nextExecuteTime;

    /** 执行次数 */
    @Excel(name = "执行次数")
    private Integer executeCount;

    /** 失败次数 */
    @Excel(name = "失败次数")
    private Integer failCount;

    /** D2：同步模式（0全量 1增量） */
    @Excel(name = "同步模式", readConverterExp = "0=全量,1=增量")
    private String syncMode;

    /** D2：增量水位（上次成功同步的截止时间点，仅成功执行后推进） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "增量水位", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastWatermark;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    @NotBlank(message = "任务名称不能为空")
    @Size(min = 0, max = 100, message = "任务名称长度不能超过100个字符")
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    @Size(min = 0, max = 50, message = "任务编码长度不能超过50个字符")
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    @NotNull(message = "外部系统ID不能为空")
    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    @NotNull(message = "接口ID不能为空")
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }

    @Size(min = 0, max = 100, message = "Cron表达式长度不能超过100个字符")
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Date getLastExecuteTime() { return lastExecuteTime; }
    public void setLastExecuteTime(Date lastExecuteTime) { this.lastExecuteTime = lastExecuteTime; }

    public Date getNextExecuteTime() { return nextExecuteTime; }
    public void setNextExecuteTime(Date nextExecuteTime) { this.nextExecuteTime = nextExecuteTime; }

    public Integer getExecuteCount() { return executeCount; }
    public void setExecuteCount(Integer executeCount) { this.executeCount = executeCount; }

    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }

    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }

    public Date getLastWatermark() { return lastWatermark; }
    public void setLastWatermark(Date lastWatermark) { this.lastWatermark = lastWatermark; }

    @Size(min = 0, max = 1, message = "状态长度不能超过1个字符")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("taskName", getTaskName())
            .append("taskCode", getTaskCode())
            .append("systemId", getSystemId())
            .append("interfaceId", getInterfaceId())
            .append("cronExpression", getCronExpression())
            .append("lastExecuteTime", getLastExecuteTime())
            .append("nextExecuteTime", getNextExecuteTime())
            .append("executeCount", getExecuteCount())
            .append("failCount", getFailCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
