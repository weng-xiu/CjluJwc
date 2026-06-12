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
 * 学籍预警对象 sam_warning
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamWarning extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预警ID */
    private Long warningId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 预警类型（0成绩预警 1学分预警 2出勤预警 3综合预警） */
    @Excel(name = "预警类型", readConverterExp = "0=成绩预警,1=学分预警,2=出勤预警,3=综合预警")
    private String warningType;

    /** 预警级别（0一般 1严重 2高危） */
    @Excel(name = "预警级别", readConverterExp = "0=一般,1=严重,2=高危")
    private String warningLevel;

    /** 预警原因 */
    @Excel(name = "预警原因")
    private String warningReason;

    /** 预警日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预警日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date warningDate;

    /** 是否解除（0否 1是） */
    @Excel(name = "是否解除", readConverterExp = "0=否,1=是")
    private String isResolved;

    /** 解除日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "解除日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date resolveDate;

    /** 解除说明 */
    @Excel(name = "解除说明")
    private String resolveRemark;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getWarningId() { return warningId; }
    public void setWarningId(Long warningId) { this.warningId = warningId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotBlank(message = "预警类型不能为空")
    @Size(min = 0, max = 1, message = "预警类型长度不能超过1个字符")
    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }

    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }

    @Size(min = 0, max = 500, message = "预警原因长度不能超过500个字符")
    public String getWarningReason() { return warningReason; }
    public void setWarningReason(String warningReason) { this.warningReason = warningReason; }

    public Date getWarningDate() { return warningDate; }
    public void setWarningDate(Date warningDate) { this.warningDate = warningDate; }

    public String getIsResolved() { return isResolved; }
    public void setIsResolved(String isResolved) { this.isResolved = isResolved; }

    public Date getResolveDate() { return resolveDate; }
    public void setResolveDate(Date resolveDate) { this.resolveDate = resolveDate; }

    public String getResolveRemark() { return resolveRemark; }
    public void setResolveRemark(String resolveRemark) { this.resolveRemark = resolveRemark; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("warningId", getWarningId())
            .append("studentId", getStudentId())
            .append("semesterId", getSemesterId())
            .append("warningType", getWarningType())
            .append("warningLevel", getWarningLevel())
            .append("warningReason", getWarningReason())
            .append("warningDate", getWarningDate())
            .append("isResolved", getIsResolved())
            .append("resolveDate", getResolveDate())
            .append("resolveRemark", getResolveRemark())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
