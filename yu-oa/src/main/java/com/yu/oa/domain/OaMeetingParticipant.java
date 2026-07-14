package com.yu.oa.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 参会人员对象 oa_meeting_participant
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaMeetingParticipant extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 参会ID */
    private Long participantId;

    /** 会议ID */
    private Long meetingId;

    /** 用户ID */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 部门名称 */
    private String deptName;

    /** 出席状态（0待确认 1参加 2不参加 3待定） */
    private String attendStatus;

    /** 签到状态（0未签到 1已签到） */
    private String signStatus;

    /** 签到时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Long getMeetingId() { return meetingId; }
    public void setMeetingId(Long meetingId) { this.meetingId = meetingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getAttendStatus() { return attendStatus; }
    public void setAttendStatus(String attendStatus) { this.attendStatus = attendStatus; }

    public String getSignStatus() { return signStatus; }
    public void setSignStatus(String signStatus) { this.signStatus = signStatus; }

    public Date getSignTime() { return signTime; }
    public void setSignTime(Date signTime) { this.signTime = signTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("participantId", getParticipantId())
            .append("meetingId", getMeetingId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("deptName", getDeptName())
            .append("attendStatus", getAttendStatus())
            .append("signStatus", getSignStatus())
            .append("signTime", getSignTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
