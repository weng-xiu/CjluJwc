package com.yu.dis.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 接口配置对象 dis_interface_config
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public class DisInterfaceConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 接口ID */
    private Long interfaceId;

    /** 所属系统ID */
    @Excel(name = "所属系统ID")
    private Long systemId;

    /** 接口名称 */
    @Excel(name = "接口名称")
    private String interfaceName;

    /** 接口编码 */
    @Excel(name = "接口编码")
    private String interfaceCode;

    /** 请求方式 */
    @Excel(name = "请求方式")
    private String requestMethod;

    /** 请求路径 */
    @Excel(name = "请求路径")
    private String requestPath;

    /** 请求模板 */
    private String requestTemplate;

    /** 响应模板 */
    private String responseTemplate;

    /** 超时时间（秒） */
    @Excel(name = "超时时间（秒）")
    private Integer timeoutSeconds;

    /** 重试次数 */
    @Excel(name = "重试次数")
    private Integer retryCount;

    /** 接口描述 */
    @Excel(name = "接口描述")
    private String description;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }

    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    @NotBlank(message = "接口名称不能为空")
    @Size(min = 0, max = 100, message = "接口名称长度不能超过100个字符")
    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

    @Size(min = 0, max = 50, message = "接口编码长度不能超过50个字符")
    public String getInterfaceCode() { return interfaceCode; }
    public void setInterfaceCode(String interfaceCode) { this.interfaceCode = interfaceCode; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    @Size(min = 0, max = 255, message = "请求路径长度不能超过255个字符")
    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getRequestTemplate() { return requestTemplate; }
    public void setRequestTemplate(String requestTemplate) { this.requestTemplate = requestTemplate; }

    public String getResponseTemplate() { return responseTemplate; }
    public void setResponseTemplate(String responseTemplate) { this.responseTemplate = responseTemplate; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    @Size(min = 0, max = 500, message = "接口描述长度不能超过500个字符")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("interfaceId", getInterfaceId())
            .append("systemId", getSystemId())
            .append("interfaceName", getInterfaceName())
            .append("interfaceCode", getInterfaceCode())
            .append("requestMethod", getRequestMethod())
            .append("requestPath", getRequestPath())
            .append("timeoutSeconds", getTimeoutSeconds())
            .append("retryCount", getRetryCount())
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