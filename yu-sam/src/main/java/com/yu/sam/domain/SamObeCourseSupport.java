package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * OBE课程支撑矩阵对象 sam_obe_course_support
 *
 * @author N1
 */
public class SamObeCourseSupport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 支撑关系ID */
    private Long supportId;

    /** 培养方案ID */
    @Excel(name = "培养方案ID")
    private Long planId;

    /** 指标点ID */
    @Excel(name = "指标点ID")
    private Long ipId;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

    /** 课程对指标点的支撑强度权重 */
    @Excel(name = "支撑权重")
    private Double supportWeight;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志 */
    private String delFlag;

    /** 课程名称（联表展示，非持久化） */
    @Excel(name = "课程名称")
    private String courseName;

    /** 指标点编号（联表展示，非持久化） */
    private String ipCode;

    public Long getSupportId() { return supportId; }
    public void setSupportId(Long supportId) { this.supportId = supportId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    @NotNull(message = "指标点ID不能为空")
    public Long getIpId() { return ipId; }
    public void setIpId(Long ipId) { this.ipId = ipId; }

    @NotNull(message = "课程ID不能为空")
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Double getSupportWeight() { return supportWeight; }
    public void setSupportWeight(Double supportWeight) { this.supportWeight = supportWeight; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getIpCode() { return ipCode; }
    public void setIpCode(String ipCode) { this.ipCode = ipCode; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("supportId", getSupportId())
            .append("planId", getPlanId())
            .append("ipId", getIpId())
            .append("courseId", getCourseId())
            .append("supportWeight", getSupportWeight())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("courseName", getCourseName())
            .append("ipCode", getIpCode())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
