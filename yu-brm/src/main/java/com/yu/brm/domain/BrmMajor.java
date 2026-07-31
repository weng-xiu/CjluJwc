package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 专业对象 brm_major
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmMajor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 专业ID */
    private Long majorId;

    /** 专业编码 */
    @Excel(name = "专业编码")
    private String majorCode;

    /** 专业名称 */
    @Excel(name = "专业名称")
    private String majorName;

    /** 所属院系ID */
    @Excel(name = "所属院系ID")
    private Long deptId;

    /** 学历层次 */
    @Excel(name = "学历层次")
    private String educationLevel;

    /** 学制（年） */
    @Excel(name = "学制")
    private Integer duration;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 所属院系名称（关联查询） */
    private String deptName;

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    @NotBlank(message = "专业编码不能为空")
    @Size(min = 0, max = 50, message = "专业编码长度不能超过50个字符")
    public String getMajorCode() { return majorCode; }
    public void setMajorCode(String majorCode) { this.majorCode = majorCode; }

    @NotBlank(message = "专业名称不能为空")
    @Size(min = 0, max = 100, message = "专业名称长度不能超过100个字符")
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    @NotNull(message = "所属院系不能为空")
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("majorId", getMajorId())
            .append("majorCode", getMajorCode())
            .append("majorName", getMajorName())
            .append("deptId", getDeptId())
            .append("educationLevel", getEducationLevel())
            .append("duration", getDuration())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
