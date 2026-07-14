package com.yu.oa.domain;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 公文对象 oa_document
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公文ID */
    private Long documentId;

    /** 文号 */
    @Excel(name = "文号")
    private String documentNo;

    /** 标题 */
    @Excel(name = "标题")
    private String title;

    /** 公文类型（0发文 1收文 2签报） */
    @Excel(name = "公文类型", readConverterExp = "0=发文,1=收文,2=签报")
    private String documentType;

    /** 密级（0公开 1内部 2秘密 3机密） */
    @Excel(name = "密级", readConverterExp = "0=公开,1=内部,2=秘密,3=机密")
    private String secretLevel;

    /** 紧急程度（0普通 1加急 2特急） */
    @Excel(name = "紧急程度", readConverterExp = "0=普通,1=加急,2=特急")
    private String urgentLevel;

    /** 正文内容 */
    private String content;

    /** 附件URL（逗号分隔） */
    private String attachments;

    /** 发起人用户ID */
    @Excel(name = "发起人用户ID")
    private Long originatorId;

    /** 发起人姓名 */
    @Excel(name = "发起人姓名")
    private String originatorName;

    /** 发起部门ID */
    private Long originDeptId;

    /** 发起部门名称 */
    @Excel(name = "发起部门名称")
    private String originDeptName;

    /** 流程实例ID */
    private Long processInstanceId;

    /** 公文状态（0草稿 1审批中 2已发布 3已归档 4已驳回） */
    @Excel(name = "公文状态", readConverterExp = "0=草稿,1=审批中,2=已发布,3=已归档,4=已驳回")
    private String documentStatus;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 附件列表 */
    private List<OaDocumentAttach> attachList;

    /** 抄送列表 */
    private List<OaDocumentCopy> copyList;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentNo() { return documentNo; }
    public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getSecretLevel() { return secretLevel; }
    public void setSecretLevel(String secretLevel) { this.secretLevel = secretLevel; }

    public String getUrgentLevel() { return urgentLevel; }
    public void setUrgentLevel(String urgentLevel) { this.urgentLevel = urgentLevel; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }

    public Long getOriginatorId() { return originatorId; }
    public void setOriginatorId(Long originatorId) { this.originatorId = originatorId; }

    public String getOriginatorName() { return originatorName; }
    public void setOriginatorName(String originatorName) { this.originatorName = originatorName; }

    public Long getOriginDeptId() { return originDeptId; }
    public void setOriginDeptId(Long originDeptId) { this.originDeptId = originDeptId; }

    public String getOriginDeptName() { return originDeptName; }
    public void setOriginDeptName(String originDeptName) { this.originDeptName = originDeptName; }

    public Long getProcessInstanceId() { return processInstanceId; }
    public void setProcessInstanceId(Long processInstanceId) { this.processInstanceId = processInstanceId; }

    public String getDocumentStatus() { return documentStatus; }
    public void setDocumentStatus(String documentStatus) { this.documentStatus = documentStatus; }

    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OaDocumentAttach> getAttachList() { return attachList; }
    public void setAttachList(List<OaDocumentAttach> attachList) { this.attachList = attachList; }

    public List<OaDocumentCopy> getCopyList() { return copyList; }
    public void setCopyList(List<OaDocumentCopy> copyList) { this.copyList = copyList; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("documentId", getDocumentId())
            .append("documentNo", getDocumentNo())
            .append("title", getTitle())
            .append("documentType", getDocumentType())
            .append("secretLevel", getSecretLevel())
            .append("urgentLevel", getUrgentLevel())
            .append("content", getContent())
            .append("attachments", getAttachments())
            .append("originatorId", getOriginatorId())
            .append("originatorName", getOriginatorName())
            .append("originDeptId", getOriginDeptId())
            .append("originDeptName", getOriginDeptName())
            .append("processInstanceId", getProcessInstanceId())
            .append("documentStatus", getDocumentStatus())
            .append("publishTime", getPublishTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
