package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 选课规则对象 tpm_selection_rule
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmSelectionRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 轮次ID */
    @Excel(name = "轮次ID")
    private Long roundId;

    /** 规则名称 */
    @Excel(name = "规则名称")
    private String ruleName;

    /** 规则类型 */
    @Excel(name = "规则类型")
    private String ruleType;

    /** 限制目标 */
    @Excel(name = "限制目标")
    private String restrictTarget;

    /** 限制值 */
    @Excel(name = "限制值")
    private String restrictValue;

    /** 优先级 */
    @Excel(name = "优先级")
    private Integer priority;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    @NotNull(message = "轮次ID不能为空")
    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    @NotBlank(message = "规则名称不能为空")
    @Size(min = 0, max = 100, message = "规则名称长度不能超过100个字符")
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    @NotBlank(message = "规则类型不能为空")
    @Size(min = 0, max = 50, message = "规则类型长度不能超过50个字符")
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public String getRestrictTarget() { return restrictTarget; }
    public void setRestrictTarget(String restrictTarget) { this.restrictTarget = restrictTarget; }

    public String getRestrictValue() { return restrictValue; }
    public void setRestrictValue(String restrictValue) { this.restrictValue = restrictValue; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("roundId", getRoundId())
            .append("ruleName", getRuleName())
            .append("ruleType", getRuleType())
            .append("restrictTarget", getRestrictTarget())
            .append("restrictValue", getRestrictValue())
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
