package com.yu.brm.domain;

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
 * 学期对象 brm_semester
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmSemester extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学期ID */
    private Long semesterId;

    /** 学期名称 */
    @Excel(name = "学期名称")
    private String semesterName;

    /** 学年ID */
    @Excel(name = "学年ID")
    private Long academicYearId;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 学期排序 */
    @Excel(name = "学期排序")
    private Integer semesterOrder;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotBlank(message = "学期名称不能为空")
    @Size(min = 0, max = 50, message = "学期名称长度不能超过50个字符")
    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    @NotNull(message = "学年ID不能为空")
    public Long getAcademicYearId() { return academicYearId; }
    public void setAcademicYearId(Long academicYearId) { this.academicYearId = academicYearId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public Integer getSemesterOrder() { return semesterOrder; }
    public void setSemesterOrder(Integer semesterOrder) { this.semesterOrder = semesterOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("semesterId", getSemesterId())
            .append("semesterName", getSemesterName())
            .append("academicYearId", getAcademicYearId())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("semesterOrder", getSemesterOrder())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
