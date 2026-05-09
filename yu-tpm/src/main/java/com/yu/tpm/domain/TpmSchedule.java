package com.yu.tpm.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 排课对象 tpm_schedule
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmSchedule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 排课ID */
    private Long scheduleId;

    /** 开课ID */
    @Excel(name = "开课ID")
    private Long offeringId;

    /** 教室ID */
    @Excel(name = "教室ID")
    private Long classroomId;

    /** 星期几（1-7） */
    @Excel(name = "星期几")
    private Integer weekDay;

    /** 开始节次 */
    @Excel(name = "开始节次")
    private Integer startPeriod;

    /** 结束节次 */
    @Excel(name = "结束节次")
    private Integer endPeriod;

    /** 起始周 */
    @Excel(name = "起始周")
    private Integer startWeek;

    /** 结束周 */
    @Excel(name = "结束周")
    private Integer endWeek;

    /** 排课方式（manual手动/auto自动） */
    @Excel(name = "排课方式")
    private String scheduleType;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    @NotNull(message = "开课ID不能为空")
    public Long getOfferingId() { return offeringId; }
    public void setOfferingId(Long offeringId) { this.offeringId = offeringId; }

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public Integer getWeekDay() { return weekDay; }
    public void setWeekDay(Integer weekDay) { this.weekDay = weekDay; }

    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }

    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }

    public Integer getStartWeek() { return startWeek; }
    public void setStartWeek(Integer startWeek) { this.startWeek = startWeek; }

    public Integer getEndWeek() { return endWeek; }
    public void setEndWeek(Integer endWeek) { this.endWeek = endWeek; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("scheduleId", getScheduleId())
            .append("offeringId", getOfferingId())
            .append("classroomId", getClassroomId())
            .append("weekDay", getWeekDay())
            .append("startPeriod", getStartPeriod())
            .append("endPeriod", getEndPeriod())
            .append("startWeek", getStartWeek())
            .append("endWeek", getEndWeek())
            .append("scheduleType", getScheduleType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
