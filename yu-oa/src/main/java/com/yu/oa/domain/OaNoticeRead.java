package com.yu.oa.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 通知公告已读记录对象 oa_notice_read
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaNoticeRead extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 已读ID */
    private Long readId;

    /** 公告ID */
    private Long noticeId;

    /** 用户ID */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    public Long getReadId() { return readId; }
    public void setReadId(Long readId) { this.readId = readId; }

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("readId", getReadId())
            .append("noticeId", getNoticeId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("readTime", getReadTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
