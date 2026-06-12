package com.yu.brm.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 教师授课资格对象 brm_teacher_qualification
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmTeacherQualification extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 资格ID */
    private Long qualId;

    /** 教师ID */
    private Long teacherId;

    /** 可授课程类别 */
    @Excel(name = "可授课程类别")
    private String courseCategory;

    /** 认证机构 */
    @Excel(name = "认证机构")
    private String certifyAuthority;

    /** 获证日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "获证日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date qualifyDate;

    /** 到期日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "到期日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expireDate;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getQualId() { return qualId; }
    public void setQualId(Long qualId) { this.qualId = qualId; }

    @NotNull(message = "教师ID不能为空")
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    @NotBlank(message = "可授课程类别不能为空")
    @Size(min = 0, max = 100, message = "可授课程类别长度不能超过100个字符")
    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public String getCertifyAuthority() { return certifyAuthority; }
    public void setCertifyAuthority(String certifyAuthority) { this.certifyAuthority = certifyAuthority; }

    public Date getQualifyDate() { return qualifyDate; }
    public void setQualifyDate(Date qualifyDate) { this.qualifyDate = qualifyDate; }

    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date expireDate) { this.expireDate = expireDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("qualId", getQualId())
            .append("teacherId", getTeacherId())
            .append("courseCategory", getCourseCategory())
            .append("certifyAuthority", getCertifyAuthority())
            .append("qualifyDate", getQualifyDate())
            .append("expireDate", getExpireDate())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
