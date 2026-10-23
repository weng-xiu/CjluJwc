package com.yu.dis.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 主数据裁决数据源优先级配置对象 dis_source_priority
 *
 * <p>F5 主数据裁决前端化：将原 {@code MasterDataPriorityPolicy} 内存硬编码的权威优先级
 * 沉淀为可配置、可审计的持久化数据。数值越大越权威，未登记源回落默认值 0。
 *
 * @author F5
 */
public class DisSourcePriority extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 优先级配置ID */
    private Long priorityId;

    /** 来源系统编码（与裁决候选 sourceCode 对齐） */
    @Excel(name = "源编码")
    private String sourceCode;

    /** 来源系统名称 */
    @Excel(name = "源名称")
    private String sourceName;

    /** 权威优先级（数值越大越权威） */
    @Excel(name = "优先级")
    private Integer priority;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getPriorityId() { return priorityId; }
    public void setPriorityId(Long priorityId) { this.priorityId = priorityId; }

    @NotBlank(message = "来源系统编码不能为空")
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    @NotNull(message = "优先级不能为空")
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("priorityId", getPriorityId())
            .append("sourceCode", getSourceCode())
            .append("sourceName", getSourceName())
            .append("priority", getPriority())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
