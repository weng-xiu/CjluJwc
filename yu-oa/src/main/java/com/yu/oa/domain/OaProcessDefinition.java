package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 流程定义快照对象 oa_process_definition
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaProcessDefinition extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 定义ID */
    private Long definitionId;

    /** 分类ID */
    @Excel(name = "分类ID")
    private Long categoryId;

    /** 流程标识 */
    @Excel(name = "流程标识")
    private String processKey;

    /** 流程名称 */
    @Excel(name = "流程名称")
    private String processName;

    /** 版本号 */
    @Excel(name = "版本号")
    private Integer version;

    /** 部署ID */
    @Excel(name = "部署ID")
    private String deploymentId;

    /** 流程定义ID */
    @Excel(name = "流程定义ID")
    private String procDefId;

    /** BPMN XML 内容 */
    private String bpmnXml;

    /** 流程说明 */
    @Excel(name = "流程说明")
    private String description;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getDefinitionId() { return definitionId; }
    public void setDefinitionId(Long definitionId) { this.definitionId = definitionId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getProcessKey() { return processKey; }
    public void setProcessKey(String processKey) { this.processKey = processKey; }

    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getDeploymentId() { return deploymentId; }
    public void setDeploymentId(String deploymentId) { this.deploymentId = deploymentId; }

    public String getProcDefId() { return procDefId; }
    public void setProcDefId(String procDefId) { this.procDefId = procDefId; }

    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("definitionId", getDefinitionId())
            .append("categoryId", getCategoryId())
            .append("processKey", getProcessKey())
            .append("processName", getProcessName())
            .append("version", getVersion())
            .append("deploymentId", getDeploymentId())
            .append("procDefId", getProcDefId())
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
