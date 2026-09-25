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
 * 学籍异动对象 sam_status_change
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamStatusChange extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 异动ID */
    private Long changeId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 异动类型（0休学 1复学 2转学 3退学 4保留学籍） */
    @Excel(name = "异动类型", readConverterExp = "0=休学,1=复学,2=转学,3=退学,4=保留学籍")
    private String changeType;

    /** 异动日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "异动日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date changeDate;

    /** 原学籍状态 */
    @Excel(name = "原学籍状态")
    private String originalStatus;

    /** 新学籍状态 */
    @Excel(name = "新学籍状态")
    private String newStatus;

    /** 申请原因 */
    @Excel(name = "申请原因")
    private String reason;

    /** 申请人 */
    @Excel(name = "申请人")
    private String applicant;

    /** 审批状态（0待审 1通过 2驳回） */
    @Excel(name = "审批状态", readConverterExp = "0=待审,1=通过,2=驳回")
    private String approveStatus;

    /** 审批人 */
    @Excel(name = "审批人")
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 审批意见 */
    @Excel(name = "审批意见")
    private String approveOpinion;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 流程实例ID（关联Flowable） */
    private String procInstId;

    /** 创建人精确检索（P6 门户本人过滤，不映射数据库列） */
    private String createByExact;

    public Long getChangeId() { return changeId; }
    public void setChangeId(Long changeId) { this.changeId = changeId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotBlank(message = "异动类型不能为空")
    @Size(min = 0, max = 1, message = "异动类型长度不能超过1个字符")
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }

    public Date getChangeDate() { return changeDate; }
    public void setChangeDate(Date changeDate) { this.changeDate = changeDate; }

    public String getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(String originalStatus) { this.originalStatus = originalStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    @Size(min = 0, max = 500, message = "申请原因长度不能超过500个字符")
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }

    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }

    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }

    public String getApproveOpinion() { return approveOpinion; }
    public void setApproveOpinion(String approveOpinion) { this.approveOpinion = approveOpinion; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProcInstId() { return procInstId; }
    public void setProcInstId(String procInstId) { this.procInstId = procInstId; }

    public String getCreateByExact() { return createByExact; }
    public void setCreateByExact(String createByExact) { this.createByExact = createByExact; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("changeId", getChangeId())
            .append("studentId", getStudentId())
            .append("changeType", getChangeType())
            .append("changeDate", getChangeDate())
            .append("originalStatus", getOriginalStatus())
            .append("newStatus", getNewStatus())
            .append("reason", getReason())
            .append("applicant", getApplicant())
            .append("approveStatus", getApproveStatus())
            .append("approveBy", getApproveBy())
            .append("approveTime", getApproveTime())
            .append("approveOpinion", getApproveOpinion())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
