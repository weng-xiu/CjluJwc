package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学分结构对象 tpm_credit_structure
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmCreditStructure extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 结构ID */
    private Long structId;

    /** 所属方案ID */
    @Excel(name = "所属方案ID")
    private Long planId;

    /** 学分类型编码 */
    @Excel(name = "学分类型编码")
    private String creditType;

    /** 学分类型名称 */
    @Excel(name = "学分类型名称")
    private String creditTypeName;

    /** 要求学分 */
    @Excel(name = "要求学分")
    private Double requiredCredit;

    /** 最低学分 */
    @Excel(name = "最低学分")
    private Double minCredit;

    /** 学分说明 */
    @Excel(name = "学分说明")
    private String description;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getStructId() { return structId; }
    public void setStructId(Long structId) { this.structId = structId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    @NotBlank(message = "学分类型编码不能为空")
    @Size(min = 0, max = 50, message = "学分类型编码长度不能超过50个字符")
    public String getCreditType() { return creditType; }
    public void setCreditType(String creditType) { this.creditType = creditType; }

    @NotBlank(message = "学分类型名称不能为空")
    @Size(min = 0, max = 100, message = "学分类型名称长度不能超过100个字符")
    public String getCreditTypeName() { return creditTypeName; }
    public void setCreditTypeName(String creditTypeName) { this.creditTypeName = creditTypeName; }

    public Double getRequiredCredit() { return requiredCredit; }
    public void setRequiredCredit(Double requiredCredit) { this.requiredCredit = requiredCredit; }

    public Double getMinCredit() { return minCredit; }
    public void setMinCredit(Double minCredit) { this.minCredit = minCredit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("structId", getStructId())
            .append("planId", getPlanId())
            .append("creditType", getCreditType())
            .append("creditTypeName", getCreditTypeName())
            .append("requiredCredit", getRequiredCredit())
            .append("minCredit", getMinCredit())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
