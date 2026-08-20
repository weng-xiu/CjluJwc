package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 人才培养方案对象 tpm_training_plan
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmTrainingPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 方案ID */
    private Long planId;

    /** 方案名称 */
    @Excel(name = "方案名称")
    private String planName;

    /** 所属专业ID */
    @Excel(name = "所属专业ID")
    private Long majorId;

    /** 所属院系ID */
    @Excel(name = "所属院系ID")
    private Long deptId;

    /** 学历层次 */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 方案年份 */
    @Excel(name = "方案年份")
    private String planYear;

    /** 总学分 */
    @Excel(name = "总学分")
    private Double totalCredits;

    /** 发布状态（0草稿 1已发布 2已废止） */
    @Excel(name = "发布状态", readConverterExp = "0=草稿,1=已发布,2=已废止")
    private String publishStatus;

    /** 发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发布日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date publishDate;

    /** 版本号 */
    @Excel(name = "版本号")
    private String version;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 所属专业名称（非持久化） */
    @Excel(name = "所属专业")
    private String majorName;

    /** 所属院系名称（非持久化） */
    @Excel(name = "所属院系")
    private String deptName;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    @NotBlank(message = "方案名称不能为空")
    @Size(min = 0, max = 200, message = "方案名称长度不能超过200个字符")
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getPlanYear() { return planYear; }
    public void setPlanYear(String planYear) { this.planYear = planYear; }

    public Double getTotalCredits() { return totalCredits; }
    public void setTotalCredits(Double totalCredits) { this.totalCredits = totalCredits; }

    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }

    public Date getPublishDate() { return publishDate; }
    public void setPublishDate(Date publishDate) { this.publishDate = publishDate; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("planId", getPlanId())
            .append("planName", getPlanName())
            .append("majorId", getMajorId())
            .append("deptId", getDeptId())
            .append("educationLevel", getEducationLevel())
            .append("planYear", getPlanYear())
            .append("totalCredits", getTotalCredits())
            .append("publishStatus", getPublishStatus())
            .append("publishDate", getPublishDate())
            .append("version", getVersion())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("majorName", getMajorName())
            .append("deptName", getDeptName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
