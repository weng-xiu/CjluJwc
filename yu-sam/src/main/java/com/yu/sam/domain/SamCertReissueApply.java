package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 证书补办申请对象 sam_cert_reissue_apply（S7）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public class SamCertReissueApply extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 申请ID */
    private Long applyId;

    /** 学生ID */
    @NotNull(message = "学生ID不能为空")
    @Excel(name = "学生ID")
    private Long studentId;

    /** 原证书ID */
    @NotNull(message = "原证书不能为空")
    private Long origCertId;

    /** 补办生成的新证书ID（受理后回填） */
    private Long newCertId;

    /** 补办原因 */
    @Excel(name = "补办原因")
    private String reason;

    /** 申请状态（0待受理 1已补办 2已驳回） */
    @Excel(name = "申请状态", readConverterExp = "0=待受理,1=已补办,2=已驳回")
    private String applyStatus;

    /** 受理人 */
    private String auditBy;

    /** 受理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 受理意见 */
    private String auditOpinion;

    /** 学生学号（联查展示字段） */
    @Excel(name = "学号")
    private String studentNo;

    /** 学生姓名（联查展示字段） */
    @Excel(name = "学生姓名")
    private String studentName;

    /** 原证书编号（联查展示字段） */
    @Excel(name = "原证书编号")
    private String origCertNumber;

    /** 原证书类型（联查展示字段） */
    private String origCertType;

    /** 补办证书编号（联查展示字段） */
    private String newCertNumber;

    public Long getApplyId() { return applyId; }
    public void setApplyId(Long applyId) { this.applyId = applyId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getOrigCertId() { return origCertId; }
    public void setOrigCertId(Long origCertId) { this.origCertId = origCertId; }

    public Long getNewCertId() { return newCertId; }
    public void setNewCertId(Long newCertId) { this.newCertId = newCertId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApplyStatus() { return applyStatus; }
    public void setApplyStatus(String applyStatus) { this.applyStatus = applyStatus; }

    public String getAuditBy() { return auditBy; }
    public void setAuditBy(String auditBy) { this.auditBy = auditBy; }

    public Date getAuditTime() { return auditTime; }
    public void setAuditTime(Date auditTime) { this.auditTime = auditTime; }

    public String getAuditOpinion() { return auditOpinion; }
    public void setAuditOpinion(String auditOpinion) { this.auditOpinion = auditOpinion; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getOrigCertNumber() { return origCertNumber; }
    public void setOrigCertNumber(String origCertNumber) { this.origCertNumber = origCertNumber; }

    public String getOrigCertType() { return origCertType; }
    public void setOrigCertType(String origCertType) { this.origCertType = origCertType; }

    public String getNewCertNumber() { return newCertNumber; }
    public void setNewCertNumber(String newCertNumber) { this.newCertNumber = newCertNumber; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("applyId", getApplyId())
            .append("studentId", getStudentId())
            .append("origCertId", getOrigCertId())
            .append("newCertId", getNewCertId())
            .append("reason", getReason())
            .append("applyStatus", getApplyStatus())
            .append("auditBy", getAuditBy())
            .append("auditTime", getAuditTime())
            .append("auditOpinion", getAuditOpinion())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("remark", getRemark())
            .toString();
    }
}
