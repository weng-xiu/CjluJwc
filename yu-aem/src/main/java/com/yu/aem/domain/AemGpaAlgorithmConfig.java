package com.yu.aem.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * GPA算法配置对象 aem_gpa_algorithm_config
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class AemGpaAlgorithmConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 算法代码 */
    @Excel(name = "算法代码")
    private String algorithmCode;

    /** 算法名称 */
    @Excel(name = "算法名称")
    private String algorithmName;

    /** 算法描述 */
    @Excel(name = "算法描述")
    private String description;

    /** 是否默认（0否 1是） */
    @Excel(name = "是否默认", readConverterExp = "0=否,1=是")
    private String isDefault;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }

    @NotBlank(message = "算法代码不能为空")
    @Size(min = 0, max = 50, message = "算法代码长度不能超过50个字符")
    public String getAlgorithmCode() { return algorithmCode; }
    public void setAlgorithmCode(String algorithmCode) { this.algorithmCode = algorithmCode; }

    @NotBlank(message = "算法名称不能为空")
    @Size(min = 0, max = 100, message = "算法名称长度不能超过100个字符")
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("algorithmCode", getAlgorithmCode())
            .append("algorithmName", getAlgorithmName())
            .append("description", getDescription())
            .append("isDefault", getIsDefault())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
