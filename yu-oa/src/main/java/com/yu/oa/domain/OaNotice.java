package com.yu.oa.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 通知公告对象 oa_notice
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告ID */
    private Long noticeId;

    /** 公告标题 */
    @Excel(name = "公告标题")
    private String noticeTitle;

    /** 公告内容 */
    private String noticeContent;

    /** 公告类型（0通知 1公告 2通报） */
    @Excel(name = "公告类型", readConverterExp = "0=通知,1=公告,2=通报")
    private String noticeType;

    /** 发布范围（0全体 1指定部门 2指定人员） */
    @Excel(name = "发布范围", readConverterExp = "0=全体,1=指定部门,2=指定人员")
    private String publishScope;

    /** 是否置顶（0否 1是） */
    @Excel(name = "是否置顶", readConverterExp = "0=否,1=是")
    private String isTop;

    /** 发布状态（0草稿 1已发布 2已撤回） */
    @Excel(name = "发布状态", readConverterExp = "0=草稿,1=已发布,2=已撤回")
    private String publishStatus;

    /** 发布人用户ID */
    @Excel(name = "发布人用户ID")
    private Long publisherId;

    /** 发布人姓名 */
    @Excel(name = "发布人姓名")
    private String publisherName;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "截止时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 阅读次数 */
    @Excel(name = "阅读次数")
    private Integer readCount;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 指定部门范围 */
    private List<OaNoticeDept> deptList;

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }

    public String getNoticeTitle() { return noticeTitle; }
    public void setNoticeTitle(String noticeTitle) { this.noticeTitle = noticeTitle; }

    public String getNoticeContent() { return noticeContent; }
    public void setNoticeContent(String noticeContent) { this.noticeContent = noticeContent; }

    public String getNoticeType() { return noticeType; }
    public void setNoticeType(String noticeType) { this.noticeType = noticeType; }

    public String getPublishScope() { return publishScope; }
    public void setPublishScope(String publishScope) { this.publishScope = publishScope; }

    public String getIsTop() { return isTop; }
    public void setIsTop(String isTop) { this.isTop = isTop; }

    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Integer getReadCount() { return readCount; }
    public void setReadCount(Integer readCount) { this.readCount = readCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OaNoticeDept> getDeptList() { return deptList; }
    public void setDeptList(List<OaNoticeDept> deptList) { this.deptList = deptList; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeId", getNoticeId())
            .append("noticeTitle", getNoticeTitle())
            .append("noticeContent", getNoticeContent())
            .append("noticeType", getNoticeType())
            .append("publishScope", getPublishScope())
            .append("isTop", getIsTop())
            .append("publishStatus", getPublishStatus())
            .append("publisherId", getPublisherId())
            .append("publisherName", getPublisherName())
            .append("publishTime", getPublishTime())
            .append("endTime", getEndTime())
            .append("readCount", getReadCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
