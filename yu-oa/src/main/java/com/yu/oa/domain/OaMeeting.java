package com.yu.oa.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 会议管理对象 oa_meeting
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaMeeting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会议ID */
    private Long meetingId;

    /** 会议主题 */
    @Excel(name = "会议主题")
    private String meetingTheme;

    /** 会议室ID */
    @Excel(name = "会议室ID")
    private Long roomId;

    /** 会议室名称 */
    @Excel(name = "会议室名称")
    private String roomName;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 组织者用户ID */
    @Excel(name = "组织者用户ID")
    private Long organizerId;

    /** 组织者姓名 */
    @Excel(name = "组织者姓名")
    private String organizerName;

    /** 会议类型（0普通会议 1视频会议 2紧急会议） */
    @Excel(name = "会议类型", readConverterExp = "0=普通会议,1=视频会议,2=紧急会议")
    private String meetingType;

    /** 会议状态（0未开始 1进行中 2已结束 3已取消） */
    @Excel(name = "会议状态", readConverterExp = "0=未开始,1=进行中,2=已结束,3=已取消")
    private String meetingStatus;

    /** 会议内容 */
    private String content;

    /** 附件URL */
    private String attachmentUrl;

    /** 流程实例ID */
    private Long processInstanceId;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 参会人员 */
    private List<OaMeetingParticipant> participantList;

    /** 会议纪要 */
    private OaMeetingMinutes minutes;

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getMeetingTheme() { return meetingTheme; }
    public void setMeetingTheme(String meetingTheme) { this.meetingTheme = meetingTheme; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getMeetingStatus() { return meetingStatus; }
    public void setMeetingStatus(String meetingStatus) { this.meetingStatus = meetingStatus; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OaMeetingParticipant> getParticipantList() { return participantList; }
    public void setParticipantList(List<OaMeetingParticipant> participantList) { this.participantList = participantList; }

    public OaMeetingMinutes getMinutes() { return minutes; }
    public void setMinutes(OaMeetingMinutes minutes) { this.minutes = minutes; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("meetingId", getMeetingId())
            .append("meetingTheme", getMeetingTheme())
            .append("roomId", getRoomId())
            .append("roomName", getRoomName())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("organizerId", getOrganizerId())
            .append("organizerName", getOrganizerName())
            .append("meetingType", getMeetingType())
            .append("meetingStatus", getMeetingStatus())
            .append("content", getContent())
            .append("attachmentUrl", getAttachmentUrl())
            .append("processInstanceId", getProcessInstanceId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
