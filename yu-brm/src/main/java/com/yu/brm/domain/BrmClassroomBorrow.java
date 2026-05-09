package com.yu.brm.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 教室借用对象 brm_classroom_borrow
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmClassroomBorrow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 借用ID */
    private Long borrowId;

    /** 教室ID */
    @NotNull(message = "教室ID不能为空")
    private Long classroomId;

    /** 申请人 */
    @Excel(name = "申请人")
    private String applicant;

    /** 申请人部门 */
    @Excel(name = "申请人部门")
    private String applicantDept;

    /** 借用日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "借用日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date borrowDate;

    /** 开始时间 */
    @Excel(name = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Excel(name = "结束时间")
    private String endTime;

    /** 借用用途 */
    @Excel(name = "借用用途")
    private String purpose;

    /** 审批状态 */
    @Excel(name = "审批状态", readConverterExp = "0=待审,1=通过,2=驳回")
    private String approveStatus;

    /** 审批人 */
    @Excel(name = "审批人")
    private String approveBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getBorrowId() { return borrowId; }
    public void setBorrowId(Long borrowId) { this.borrowId = borrowId; }

    @NotNull(message = "教室ID不能为空")
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    @NotBlank(message = "申请人不能为空")
    public String getApplicant() { return applicant; }
    public void setApplicant(String applicant) { this.applicant = applicant; }

    public String getApplicantDept() { return applicantDept; }
    public void setApplicantDept(String applicantDept) { this.applicantDept = applicantDept; }

    @NotNull(message = "借用日期不能为空")
    public Date getBorrowDate() { return borrowDate; }
    public void setBorrowDate(Date borrowDate) { this.borrowDate = borrowDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getApproveStatus() { return approveStatus; }
    public void setApproveStatus(String approveStatus) { this.approveStatus = approveStatus; }

    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String approveBy) { this.approveBy = approveBy; }

    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date approveTime) { this.approveTime = approveTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("borrowId", getBorrowId())
            .append("classroomId", getClassroomId())
            .append("applicant", getApplicant())
            .append("applicantDept", getApplicantDept())
            .append("borrowDate", getBorrowDate())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("purpose", getPurpose())
            .append("approveStatus", getApproveStatus())
            .append("approveBy", getApproveBy())
            .append("approveTime", getApproveTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
