package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * OBE指标点对象 sam_obe_indicator_point
 *
 * @author N1
 */
public class SamObeIndicatorPoint extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 指标点ID */
    private Long ipId;

    /** 所属毕业要求ID */
    @Excel(name = "毕业要求ID")
    private Long grId;

    /** 培养方案ID */
    @Excel(name = "培养方案ID")
    private Long planId;

    /** 指标点编号 */
    @Excel(name = "指标点编号")
    private String ipCode;

    /** 指标点名称 */
    @Excel(name = "指标点名称")
    private String ipName;

    /** 指标点描述 */
    @Excel(name = "指标点描述")
    private String ipDesc;

    /** 在毕业要求中的权重 */
    @Excel(name = "权重")
    private Double weight;

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

    public Long getIpId() { return ipId; }
    public void setIpId(Long ipId) { this.ipId = ipId; }

    @NotNull(message = "毕业要求ID不能为空")
    public Long getGrId() { return grId; }
    public void setGrId(Long grId) { this.grId = grId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    @NotNull(message = "指标点编号不能为空")
    public String getIpCode() { return ipCode; }
    public void setIpCode(String ipCode) { this.ipCode = ipCode; }

    @NotNull(message = "指标点名称不能为空")
    public String getIpName() { return ipName; }
    public void setIpName(String ipName) { this.ipName = ipName; }

    public String getIpDesc() { return ipDesc; }
    public void setIpDesc(String ipDesc) { this.ipDesc = ipDesc; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

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
            .append("ipId", getIpId())
            .append("grId", getGrId())
            .append("planId", getPlanId())
            .append("ipCode", getIpCode())
            .append("ipName", getIpName())
            .append("ipDesc", getIpDesc())
            .append("weight", getWeight())
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
