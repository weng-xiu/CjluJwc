package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;

/**
 * 日程共享人员对象 oa_schedule_share
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaScheduleShare extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 共享ID */
    private Long shareId;

    /** 日程ID */
    private Long scheduleId;

    /** 共享用户ID */
    private Long userId;

    /** 共享用户姓名 */
    private String userName;

    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("shareId", getShareId())
            .append("scheduleId", getScheduleId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
