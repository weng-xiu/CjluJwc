package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;

/**
 * 公文附件对象 oa_document_attach
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaDocumentAttach extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 附件ID */
    private Long attachId;

    /** 公文ID */
    private Long documentId;

    /** 文件名称 */
    private String fileName;

    /** 文件路径 */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    public Long getAttachId() { return attachId; }
    public void setAttachId(Long attachId) { this.attachId = attachId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("attachId", getAttachId())
            .append("documentId", getDocumentId())
            .append("fileName", getFileName())
            .append("fileUrl", getFileUrl())
            .append("fileSize", getFileSize())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
