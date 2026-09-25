package com.yu.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 电子凭证发放记录 sys_print_record
 *
 * @author yu
 * @date 2026-09-25
 */
public class SysPrintRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    @Excel(name = "凭证ID", cellType = Excel.ColumnType.NUMERIC)
    private Long recordId;

    /** 凭证业务类型 */
    @Excel(name = "凭证类型")
    private String bizType;

    /** 业务主键ID */
    private Long bizId;

    /** 凭证标题 */
    @Excel(name = "凭证标题")
    private String title;

    /** 凭证编号 */
    @Excel(name = "凭证编号")
    private String serialNo;

    /** 验证码 */
    @Excel(name = "验证码")
    private String verifyCode;

    /** 数据快照（JSON） */
    private String snapshot;

    /** 快照SHA256 */
    private String dataHash;

    /** 使用模板编码 */
    private String templateCode;

    /** 接收人ID（sys_user） */
    private Long receiveId;

    /** 接收人名称 */
    @Excel(name = "接收人")
    private String receiveName;

    /** 发放渠道（0管理端 1门户） */
    @Excel(name = "发放渠道", readConverterExp = "0=管理端,1=门户")
    private String issueChannel;

    /** 发放人 */
    @Excel(name = "发放人")
    private String issueBy;

    /** 发放时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发放时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date issueTime;

    /** 状态（0有效 1已作废） */
    @Excel(name = "状态", readConverterExp = "0=有效,1=已作废")
    private String status;

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getRecordId()
    {
        return recordId;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public void setSerialNo(String serialNo)
    {
        this.serialNo = serialNo;
    }

    public String getSerialNo()
    {
        return serialNo;
    }

    public void setVerifyCode(String verifyCode)
    {
        this.verifyCode = verifyCode;
    }

    public String getVerifyCode()
    {
        return verifyCode;
    }

    public void setSnapshot(String snapshot)
    {
        this.snapshot = snapshot;
    }

    public String getSnapshot()
    {
        return snapshot;
    }

    public void setDataHash(String dataHash)
    {
        this.dataHash = dataHash;
    }

    public String getDataHash()
    {
        return dataHash;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setReceiveId(Long receiveId)
    {
        this.receiveId = receiveId;
    }

    public Long getReceiveId()
    {
        return receiveId;
    }

    public void setReceiveName(String receiveName)
    {
        this.receiveName = receiveName;
    }

    public String getReceiveName()
    {
        return receiveName;
    }

    public void setIssueChannel(String issueChannel)
    {
        this.issueChannel = issueChannel;
    }

    public String getIssueChannel()
    {
        return issueChannel;
    }

    public void setIssueBy(String issueBy)
    {
        this.issueBy = issueBy;
    }

    public String getIssueBy()
    {
        return issueBy;
    }

    public void setIssueTime(Date issueTime)
    {
        this.issueTime = issueTime;
    }

    public Date getIssueTime()
    {
        return issueTime;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("bizType", getBizType())
            .append("bizId", getBizId())
            .append("title", getTitle())
            .append("serialNo", getSerialNo())
            .append("verifyCode", getVerifyCode())
            .append("templateCode", getTemplateCode())
            .append("receiveId", getReceiveId())
            .append("receiveName", getReceiveName())
            .append("issueChannel", getIssueChannel())
            .append("issueBy", getIssueBy())
            .append("issueTime", getIssueTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
