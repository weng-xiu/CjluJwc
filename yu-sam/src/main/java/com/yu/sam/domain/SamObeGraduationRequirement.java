package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * OBE毕业要求对象 sam_obe_graduation_requirement
 *
 * @author N1
 */
public class SamObeGraduationRequirement extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 毕业要求ID */
    private Long grId;

    /** 培养方案ID */
    @Excel(name = "培养方案ID")
    private Long planId;

    /** 毕业要求编号 */
    @Excel(name = "毕业要求编号")
    private String grCode;

    /** 毕业要求名称 */
    @Excel(name = "毕业要求名称")
    private String grName;

    /** 毕业要求描述 */
    @Excel(name = "毕业要求描述")
    private String grDesc;

    /** 达成度目标值（0~1） */
    @Excel(name = "达成度目标值")
    private Double targetValue;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志 */
    private String delFlag;

    public Long getGrId() { return grId; }
    public void setGrId(Long grId) { this.grId = grId; }

    @NotNull(message = "培养方案ID不能为空")
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    @NotNull(message = "毕业要求编号不能为空")
    public String getGrCode() { return grCode; }
    public void setGrCode(String grCode) { this.grCode = grCode; }

    @NotNull(message = "毕业要求名称不能为空")
    public String getGrName() { return grName; }
    public void setGrName(String grName) { this.grName = grName; }

    public String getGrDesc() { return grDesc; }
    public void setGrDesc(String grDesc) { this.grDesc = grDesc; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("grId", getGrId())
            .append("planId", getPlanId())
            .append("grCode", getGrCode())
            .append("grName", getGrName())
            .append("grDesc", getGrDesc())
            .append("targetValue", getTargetValue())
            .append("sortOrder", getSortOrder())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
