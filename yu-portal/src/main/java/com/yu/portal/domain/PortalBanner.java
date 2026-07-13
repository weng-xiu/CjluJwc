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
 * 门户轮播对象 portal_banner
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public class PortalBanner extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 轮播ID */
    private Long bannerId;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 图片URL */
    @Excel(name = "图片URL")
    private String imageUrl;

    /** 点击跳转链接 */
    @Excel(name = "跳转链接")
    private String linkUrl;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 是否激活(0否 1是) */
    @Excel(name = "是否激活", readConverterExp = "0=否,1=是")
    private String isActive;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    public Long getBannerId() { return bannerId; }
    public void setBannerId(Long bannerId) { this.bannerId = bannerId; }

    @NotBlank(message = "标题不能为空")
    @Size(min = 0, max = 200, message = "标题长度不能超过200个字符")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @NotBlank(message = "图片URL不能为空")
    @Size(min = 0, max = 500, message = "图片URL长度不能超过500个字符")
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Size(min = 0, max = 500, message = "跳转链接长度不能超过500个字符")
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getIsActive() { return isActive; }
    public void setIsActive(String isActive) { this.isActive = isActive; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("bannerId", getBannerId())
            .append("title", getTitle())
            .append("imageUrl", getImageUrl())
            .append("linkUrl", getLinkUrl())
            .append("sortOrder", getSortOrder())
            .append("isActive", getIsActive())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
