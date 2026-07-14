package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;

/**
 * 会议纪要对象 oa_meeting_minutes
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaMeetingMinutes extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 纪要ID */
    private Long minutesId;

    /** 会议ID */
    private Long meetingId;

    /** 纪要内容 */
    private String content;

    /** 记录人用户ID */
    private Long recorderId;

    /** 记录人姓名 */
    private String recorderName;

    /** 下发状态（0未下发 1已下发） */
    private String issueStatus;

    /** 状态（0正常 1停用） */
    private String status;

    public Long getMinutesId() { return minutesId; }
    public void setMinutesId(Long minutesId) { this.minutesId = minutesId; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRecorderId() { return recorderId; }
    public void setRecorderId(Long recorderId) { this.recorderId = recorderId; }

    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }

    public String getIssueStatus() { return issueStatus; }
    public void setIssueStatus(String issueStatus) { this.issueStatus = issueStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("minutesId", getMinutesId())
            .append("meetingId", getMeetingId())
            .append("content", getContent())
            .append("recorderId", getRecorderId())
            .append("recorderName", getRecorderName())
            .append("issueStatus", getIssueStatus())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
