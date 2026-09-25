package com.yu.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;
import com.yu.common.xss.Xss;

/**
 * 打印凭证模板 sys_print_template
 *
 * @author yu
 * @date 2026-09-25
 */
public class SysPrintTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long templateId;

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 业务类型（TRANSCRIPT成绩单 CERTIFICATE证书 EXAM_TICKET准考证 SCHEDULE课表 INVIGILATION监考通知单） */
    private String bizType;

    /** 模板内容（Velocity HTML） */
    private String content;

    /** 状态（0正常 1停用） */
    private String status;

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateCode(String templateCode)
    {
        this.templateCode = templateCode;
    }

    @NotBlank(message = "模板编码不能为空")
    @Size(min = 0, max = 50, message = "模板编码长度不能超过50个字符")
    public String getTemplateCode()
    {
        return templateCode;
    }

    public void setTemplateName(String templateName)
    {
        this.templateName = templateName;
    }

    @NotBlank(message = "模板名称不能为空")
    @Size(min = 0, max = 100, message = "模板名称长度不能超过100个字符")
    @Xss(message = "模板名称不能包含脚本字符")
    public String getTemplateName()
    {
        return templateName;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    @NotBlank(message = "业务类型不能为空")
    @Size(min = 0, max = 30, message = "业务类型长度不能超过30个字符")
    public String getBizType()
    {
        return bizType;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent()
    {
        return content;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("templateId", getTemplateId())
            .append("templateCode", getTemplateCode())
            .append("templateName", getTemplateName())
            .append("bizType", getBizType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
