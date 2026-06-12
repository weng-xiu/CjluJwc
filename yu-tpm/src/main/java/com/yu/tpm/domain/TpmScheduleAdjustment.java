package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 调停课申请对象 tpm_schedule_adjustment
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmScheduleAdjustment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 调整ID */
    private Long adjustId;

    /** 排课ID */
    @Excel(name = "排课ID")
    private Long scheduleId;

    /** 调整类型 */
    @Excel(name = "调整类型")
    private String adjustType;

    /** 原日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "原日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date originalDate;

    /** 新日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "新日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date newDate;

    /** 新教室ID */
    @Excel(name = "新教室ID")
    private Long newClassroomId;

    /** 新星期几 */
    @Excel(name = "新星期几")
    private Integer newWeekDay;

    /** 新开始节次 */
    @Excel(name = "新开始节次")
    private Integer newStartPeriod;

    /** 新结束节次 */
    @Excel(name = "新结束节次")
    private Integer newEndPeriod;

    /** 申请原因 */
    @Excel(name = "申请原因")
    private String reason;

    /** 申请人 */
    @Excel(name = "申请人")
    private String applicant;

    /** 审批状态（0待审 1通过 2驳回） */
    @Excel(name = "审批状态", readConverterExp = "0=待审,1=通过,2=驳回")
    private String approveStatus;

    /** 审批人 */
    @Excel(name = "审批人")
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getAdjustId() { return adjustId; }
    public void setAdjustId(Long adjustId) { this.adjustId = adjustId; }

    @NotNull(message = "排课ID不能为空")
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    @NotBlank(message = "调整类型不能为空")
    @Size(min = 0, max = 20, message = "调整类型长度不能超过20个字符")
    public String getAdjustType() { return adjustType; }
    public void setAdjustType(String adjustType) { this.adjustType = adjustType; }

    public Date getOriginalDate() { return originalDate; }
    public void setOriginalDate(Date originalDate) { this.originalDate = originalDate; }

    public Date getNewDate() { return newDate; }
    public void setNewDate(Date newDate) { this.newDate = newDate; }

    public Long getNewClassroomId() { return newClassroomId; }
    public void setNewClassroomId(Long newClassroomId) { this.newClassroomId = newClassroomId; }

    public Integer getNewWeekDay() { return newWeekDay; }
    public void setNewWeekDay(Integer newWeekDay) { this.newWeekDay = newWeekDay; }

    public Integer getNewStartPeriod() { return newStartPeriod; }
    public void setNewStartPeriod(Integer newStartPeriod) { this.newStartPeriod = newStartPeriod; }

    public Integer getNewEndPeriod() { return newEndPeriod; }
    public void setNewEndPeriod(Integer newEndPeriod) { this.newEndPeriod = newEndPeriod; }

    @NotBlank(message = "申请原因不能为空")
    @Size(min = 0, max = 500, message = "申请原因长度不能超过500个字符")
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }

    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }

    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("adjustId", getAdjustId())
            .append("scheduleId", getScheduleId())
            .append("adjustType", getAdjustType())
            .append("originalDate", getOriginalDate())
            .append("newDate", getNewDate())
            .append("newClassroomId", getNewClassroomId())
            .append("newWeekDay", getNewWeekDay())
            .append("newStartPeriod", getNewStartPeriod())
            .append("newEndPeriod", getNewEndPeriod())
            .append("reason", getReason())
            .append("applicant", getApplicant())
            .append("approveStatus", getApproveStatus())
            .append("approveBy", getApproveBy())
            .append("approveTime", getApproveTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
