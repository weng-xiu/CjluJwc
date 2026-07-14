package com.yu.oa.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 公文抄送对象 oa_document_copy
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaDocumentCopy extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 抄送ID */
    private Long copyId;

    /** 公文ID */
    private Long documentId;

    /** 抄送人用户ID */
    private Long userId;

    /** 抄送人姓名 */
    private String userName;

    /** 阅读状态（0未读 1已读） */
    private String readStatus;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("copyId", getCopyId())
            .append("documentId", getDocumentId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("readStatus", getReadStatus())
            .append("readTime", getReadTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
