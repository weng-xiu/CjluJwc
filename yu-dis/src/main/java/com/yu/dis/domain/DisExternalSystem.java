package com.yu.dis.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 外部系统配置对象 dis_external_system
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public class DisExternalSystem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 系统ID */
    private Long systemId;

    /** 系统名称 */
    @Excel(name = "系统名称")
    private String systemName;

    /** 系统编码 */
    @Excel(name = "系统编码")
    private String systemCode;

    /** 系统类型 */
    @Excel(name = "系统类型", readConverterExp = "GRADUATE=研究生系统,FINANCE=财务系统,CARD=一卡通系统,AUTH=统一身份认证平台")
    private String systemType;

    /** 基础URL */
    @Excel(name = "基础URL")
    private String baseUrl;

    /** 认证方式 */
    @Excel(name = "认证方式", readConverterExp = "TOKEN=Token认证,BASIC=Basic认证,OAUTH2=OAuth2认证,NONE=无认证")
    private String authType;

    /** 认证配置 */
    private String authConfig;

    /** 系统描述 */
    @Excel(name = "系统描述")
    private String description;

    /** 对接负责人 */
    @Excel(name = "对接负责人")
    private String contactName;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String contactPhone;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    @NotBlank(message = "系统名称不能为空")
    @Size(min = 0, max = 100, message = "系统名称长度不能超过100个字符")
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }

    @NotBlank(message = "系统编码不能为空")
    @Size(min = 0, max = 50, message = "系统编码长度不能超过50个字符")
    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }

    @NotBlank(message = "系统类型不能为空")
    public String getSystemType() { return systemType; }
    public void setSystemType(String systemType) { this.systemType = systemType; }

    @Size(min = 0, max = 255, message = "基础URL长度不能超过255个字符")
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getAuthConfig() { return authConfig; }
    public void setAuthConfig(String authConfig) { this.authConfig = authConfig; }

    @Size(min = 0, max = 500, message = "系统描述长度不能超过500个字符")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @Size(min = 0, max = 50, message = "对接负责人长度不能超过50个字符")
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    @Size(min = 0, max = 20, message = "联系电话长度不能超过20个字符")
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("systemId", getSystemId())
            .append("systemName", getSystemName())
            .append("systemCode", getSystemCode())
            .append("systemType", getSystemType())
            .append("baseUrl", getBaseUrl())
            .append("authType", getAuthType())
            .append("authConfig", getAuthConfig())
            .append("description", getDescription())
            .append("contactName", getContactName())
            .append("contactPhone", getContactPhone())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}