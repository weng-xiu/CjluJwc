package com.yu.portal.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 教务通知对象 portal_notice
 *
 * @author ruoyi
 * @date 2026-05-20
 */
public class PortalNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 通知ID */
    private Long noticeId;

    /** 通知标题 */
    @Excel(name = "通知标题")
    private String noticeTitle;

    /** 通知类型（1选课通知 2考试通知 3学籍通知 4综合通知） */
    @Excel(name = "通知类型", readConverterExp = "1=选课通知,2=考试通知,3=学籍通知,4=综合通知")
    private String noticeType;

    /** 通知内容 */
    @Excel(name = "通知内容")
    private String noticeContent;

    /** 发布部门ID */
    @Excel(name = "发布部门ID")
    private Long publishDeptId;

    /** 发布部门名称 */
    @Excel(name = "发布部门名称")
    private String publishDeptName;

    /** 发布状态（0草稿 1已发布 2已撤回） */
    @Excel(name = "发布状态", readConverterExp = "0=草稿,1=已发布,2=已撤回")
    private String publishStatus;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发布日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date publishDate;

    /** 目标角色（0所有人 1学生 2教师） */
    @Excel(name = "目标角色", readConverterExp = "0=所有人,1=学生,2=教师")
    private String targetRole;

    /** 是否置顶（0否 1是） */
    @Excel(name = "是否置顶", readConverterExp = "0=否,1=是")
    private String isTop;

    /** 浏览次数 */
    @Excel(name = "浏览次数")
    private Integer viewCount;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }

    @NotBlank(message = "通知标题不能为空")
    @Size(min = 0, max = 200, message = "通知标题长度不能超过200个字符")
    public String getNoticeTitle() { return noticeTitle; }
    public void setNoticeTitle(String noticeTitle) { this.noticeTitle = noticeTitle; }

    @NotBlank(message = "通知类型不能为空")
    @Size(min = 0, max = 1, message = "通知类型长度不能超过1个字符")
    public String getNoticeType() { return noticeType; }
    public void setNoticeType(String noticeType) { this.noticeType = noticeType; }

    public String getNoticeContent() { return noticeContent; }
    public void setNoticeContent(String noticeContent) { this.noticeContent = noticeContent; }

    public Long getPublishDeptId() { return publishDeptId; }
    public void setPublishDeptId(Long publishDeptId) { this.publishDeptId = publishDeptId; }

    public String getPublishDeptName() { return publishDeptName; }
    public void setPublishDeptName(String publishDeptName) { this.publishDeptName = publishDeptName; }

    @NotBlank(message = "发布状态不能为空")
    @Size(min = 0, max = 1, message = "发布状态长度不能超过1个字符")
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }

    public Date getPublishDate() { return publishDate; }
    public void setPublishDate(Date publishDate) { this.publishDate = publishDate; }

    @NotBlank(message = "目标角色不能为空")
    @Size(min = 0, max = 1, message = "目标角色长度不能超过1个字符")
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    @NotBlank(message = "是否置顶不能为空")
    @Size(min = 0, max = 1, message = "是否置顶长度不能超过1个字符")
    public String getIsTop() { return isTop; }
    public void setIsTop(String isTop) { this.isTop = isTop; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    @Size(min = 0, max = 1, message = "状态长度不能超过1个字符")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeId", getNoticeId())
            .append("noticeTitle", getNoticeTitle())
            .append("noticeType", getNoticeType())
            .append("noticeContent", getNoticeContent())
            .append("publishDeptId", getPublishDeptId())
            .append("publishDeptName", getPublishDeptName())
            .append("publishStatus", getPublishStatus())
            .append("publishDate", getPublishDate())
            .append("targetRole", getTargetRole())
            .append("isTop", getIsTop())
            .append("viewCount", getViewCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
