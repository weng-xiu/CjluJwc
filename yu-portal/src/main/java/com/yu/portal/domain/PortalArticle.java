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
 * 门户文章对象 portal_article
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public class PortalArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private Long articleId;

    /** 所属栏目ID */
    @Excel(name = "所属栏目ID")
    private Long columnId;

    /** 文章标题 */
    @Excel(name = "文章标题")
    private String title;

    /** 摘要 */
    @Excel(name = "摘要")
    private String summary;

    /** 文章内容(富文本) */
    private String content;

    /** 封面图URL */
    @Excel(name = "封面图URL")
    private String coverUrl;

    /** 来源 */
    @Excel(name = "来源")
    private String source;

    /** 作者 */
    @Excel(name = "作者")
    private String author;

    /** 发布状态(0草稿 1待审核 2已发布 3已撤回) */
    @Excel(name = "发布状态", readConverterExp = "0=草稿,1=待审核,2=已发布,3=已撤回")
    private String publishStatus;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishDate;

    /** 是否置顶(0否 1是) */
    @Excel(name = "是否置顶", readConverterExp = "0=否,1=是")
    private String isTop;

    /** 是否推荐到首页(0否 1是) */
    @Excel(name = "是否推荐", readConverterExp = "0=否,1=是")
    private String isFeatured;

    /** 浏览次数 */
    @Excel(name = "浏览次数")
    private Integer viewCount;

    /** 审核人ID */
    @Excel(name = "审核人ID")
    private Long reviewerId;

    /** 审核意见 */
    @Excel(name = "审核意见")
    private String reviewComment;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date reviewTime;

    /** 栏目名称（非持久化，用于列表展示） */
    @Excel(name = "栏目名称")
    private String columnName;

    /** 栏目编码（非持久化，用于前台面包屑跳转） */
    private String columnCode;

    public Long getArticleId() { return articleId; }
    public void setArticleId(Long articleId) { this.articleId = articleId; }

    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 0, max = 200, message = "文章标题长度不能超过200个字符")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Size(min = 0, max = 500, message = "摘要长度不能超过500个字符")
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Size(min = 0, max = 500, message = "封面图URL长度不能超过500个字符")
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    @Size(min = 0, max = 100, message = "来源长度不能超过100个字符")
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    @Size(min = 0, max = 64, message = "作者长度不能超过64个字符")
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }

    public Date getPublishDate() { return publishDate; }
    public void setPublishDate(Date publishDate) { this.publishDate = publishDate; }

    public String getIsTop() { return isTop; }
    public void setIsTop(String isTop) { this.isTop = isTop; }

    public String getIsFeatured() { return isFeatured; }
    public void setIsFeatured(String isFeatured) { this.isFeatured = isFeatured; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

    @Size(min = 0, max = 500, message = "审核意见长度不能超过500个字符")
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Date getReviewTime() { return reviewTime; }
    public void setReviewTime(Date reviewTime) { this.reviewTime = reviewTime; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getColumnCode() { return columnCode; }
    public void setColumnCode(String columnCode) { this.columnCode = columnCode; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("articleId", getArticleId())
            .append("columnId", getColumnId())
            .append("title", getTitle())
            .append("summary", getSummary())
            .append("content", getContent())
            .append("coverUrl", getCoverUrl())
            .append("source", getSource())
            .append("author", getAuthor())
            .append("publishStatus", getPublishStatus())
            .append("publishDate", getPublishDate())
            .append("isTop", getIsTop())
            .append("isFeatured", getIsFeatured())
            .append("viewCount", getViewCount())
            .append("reviewerId", getReviewerId())
            .append("reviewComment", getReviewComment())
            .append("reviewTime", getReviewTime())
            .append("columnName", getColumnName())
            .append("columnCode", getColumnCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
