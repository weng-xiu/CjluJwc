package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 证书编号管理对象 sam_certificate
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamCertificate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 证书ID */
    private Long certId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 证书类型（0毕业证书 1学位证书 2结业证书） */
    @Excel(name = "证书类型", readConverterExp = "0=毕业证书,1=学位证书,2=结业证书")
    private String certType;

    /** 证书编号 */
    @Excel(name = "证书编号")
    private String certNumber;

    /** 发证日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发证日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date certDate;

    /** 专业ID */
    @Excel(name = "专业ID")
    private Long majorId;

    /** 学历层次 */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 是否发放（0否 1是） */
    @Excel(name = "是否发放", readConverterExp = "0=否,1=是")
    private String isIssued;

    /** 发放日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发放日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date issueDate;

    /** 领取人 */
    @Excel(name = "领取人")
    private String receiver;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getCertId() { return certId; }
    public void setCertId(Long certId) { this.certId = certId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotBlank(message = "证书类型不能为空")
    @Size(min = 0, max = 1, message = "证书类型长度不能超过1个字符")
    public String getCertType() { return certType; }
    public void setCertType(String certType) { this.certType = certType; }

    @NotBlank(message = "证书编号不能为空")
    @Size(min = 0, max = 50, message = "证书编号长度不能超过50个字符")
    public String getCertNumber() { return certNumber; }
    public void setCertNumber(String certNumber) { this.certNumber = certNumber; }

    public Date getCertDate() { return certDate; }
    public void setCertDate(Date certDate) { this.certDate = certDate; }

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getIsIssued() { return isIssued; }
    public void setIsIssued(String isIssued) { this.isIssued = isIssued; }

    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("certId", getCertId())
            .append("studentId", getStudentId())
            .append("certType", getCertType())
            .append("certNumber", getCertNumber())
            .append("certDate", getCertDate())
            .append("majorId", getMajorId())
            .append("educationLevel", getEducationLevel())
            .append("isIssued", getIsIssued())
            .append("issueDate", getIssueDate())
            .append("receiver", getReceiver())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
