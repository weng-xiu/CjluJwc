package com.yu.dis.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 数据对接质量异常工单对象 dis_quality_ticket（F5 质量看板异常工单化）。
 *
 * <p>把质量看板（N2）识别出的同步任务异常沉淀为可指派、可跟踪、可闭环的处置工单，
 * 支持异常检测自动生成与人工登记，含处置状态机（待处理→处理中→已解决/已关闭）与结论回填。
 *
 * @author F5
 */
public class DisQualityTicket extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long ticketId;

    /** 关联同步任务ID（可空=人工登记） */
    @Excel(name = "同步任务ID")
    private Long taskId;

    /** 任务名称（快照） */
    @Excel(name = "任务名称")
    private String taskName;

    /** 异常类型（0失败率超标 1连续失败 2长时间未同步 3其他） */
    @Excel(name = "异常类型", readConverterExp = "0=失败率超标,1=连续失败,2=长时间未同步,3=其他")
    private String anomalyType;

    /** 严重程度（0低 1中 2高） */
    @Excel(name = "严重程度", readConverterExp = "0=低,1=中,2=高")
    private String severity;

    /** 异常描述 */
    @Excel(name = "异常描述")
    private String anomalyDetail;

    /** 生成时失败率（百分比） */
    @Excel(name = "失败率(%)")
    private Double failRate;

    /** 处置状态（0待处理 1处理中 2已解决 3已关闭） */
    @Excel(name = "处置状态", readConverterExp = "0=待处理,1=处理中,2=已解决,3=已关闭")
    private String status;

    /** 处置责任人 */
    @Excel(name = "处置责任人")
    private String assignee;

    /** 处置结论 */
    @Excel(name = "处置结论")
    private String resolution;

    /** 解决时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "解决时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date resolvedTime;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    @Size(min = 0, max = 100, message = "任务名称长度不能超过100个字符")
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    @NotBlank(message = "异常类型不能为空")
    public String getAnomalyType() { return anomalyType; }
    public void setAnomalyType(String anomalyType) { this.anomalyType = anomalyType; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    @Size(min = 0, max = 500, message = "异常描述长度不能超过500个字符")
    public String getAnomalyDetail() { return anomalyDetail; }
    public void setAnomalyDetail(String anomalyDetail) { this.anomalyDetail = anomalyDetail; }

    public Double getFailRate() { return failRate; }
    public void setFailRate(Double failRate) { this.failRate = failRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Size(min = 0, max = 64, message = "处置责任人长度不能超过64个字符")
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    @Size(min = 0, max = 500, message = "处置结论长度不能超过500个字符")
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Date getResolvedTime() { return resolvedTime; }
    public void setResolvedTime(Date resolvedTime) { this.resolvedTime = resolvedTime; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ticketId", getTicketId())
            .append("taskId", getTaskId())
            .append("taskName", getTaskName())
            .append("anomalyType", getAnomalyType())
            .append("severity", getSeverity())
            .append("anomalyDetail", getAnomalyDetail())
            .append("failRate", getFailRate())
            .append("status", getStatus())
            .append("assignee", getAssignee())
            .append("resolution", getResolution())
            .append("resolvedTime", getResolvedTime())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
