package com.yu.oa.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 日程安排对象 oa_schedule
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaSchedule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日程ID */
    private Long scheduleId;

    /** 日程标题 */
    @Excel(name = "日程标题")
    private String scheduleTitle;

    /** 日程类型（0个人 1会议 2任务 3提醒） */
    @Excel(name = "日程类型", readConverterExp = "0=个人,1=会议,2=任务,3=提醒")
    private String scheduleType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 是否全天（0否 1是） */
    @Excel(name = "是否全天", readConverterExp = "0=否,1=是")
    private String allDay;

    /** 提醒方式（0不提醒 1系统消息 2邮件 3短信） */
    @Excel(name = "提醒方式", readConverterExp = "0=不提醒,1=系统消息,2=邮件,3=短信")
    private String remindType;

    /** 提醒时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提醒时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date remindTime;

    /** 颜色标记 */
    @Excel(name = "颜色标记")
    private String color;

    /** 地点 */
    @Excel(name = "地点")
    private String location;

    /** 日程内容 */
    private String scheduleContent;

    /** 所属人用户ID */
    @Excel(name = "所属人用户ID")
    private Long ownerId;

    /** 所属人姓名 */
    @Excel(name = "所属人姓名")
    private String ownerName;

    /** 日程状态（0正常 1已完成 2已取消） */
    @Excel(name = "日程状态", readConverterExp = "0=正常,1=已完成,2=已取消")
    private String scheduleStatus;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 共享人员 */
    private List<OaScheduleShare> shareList;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public String getScheduleTitle() { return scheduleTitle; }
    public void setScheduleTitle(String scheduleTitle) { this.scheduleTitle = scheduleTitle; }

    public String getScheduleType() { return scheduleType; }
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public String getAllDay() { return allDay; }
    public void setAllDay(String allDay) { this.allDay = allDay; }

    public String getRemindType() { return remindType; }
    public void setRemindType(String remindType) { this.remindType = remindType; }

    public Date getRemindTime() { return remindTime; }
    public void setRemindTime(Date remindTime) { this.remindTime = remindTime; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getScheduleContent() { return scheduleContent; }
    public void setScheduleContent(String scheduleContent) { this.scheduleContent = scheduleContent; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getScheduleStatus() { return scheduleStatus; }
    public void setScheduleStatus(String scheduleStatus) { this.scheduleStatus = scheduleStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OaScheduleShare> getShareList() { return shareList; }
    public void setShareList(List<OaScheduleShare> shareList) { this.shareList = shareList; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("scheduleId", getScheduleId())
            .append("scheduleTitle", getScheduleTitle())
            .append("scheduleType", getScheduleType())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("allDay", getAllDay())
            .append("remindType", getRemindType())
            .append("remindTime", getRemindTime())
            .append("color", getColor())
            .append("location", getLocation())
            .append("scheduleContent", getScheduleContent())
            .append("ownerId", getOwnerId())
            .append("ownerName", getOwnerName())
            .append("scheduleStatus", getScheduleStatus())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
