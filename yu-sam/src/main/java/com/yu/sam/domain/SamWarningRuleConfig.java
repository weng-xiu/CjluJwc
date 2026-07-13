package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 预警规则配置对象 sam_warning_rule_config
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class SamWarningRuleConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 规则代码(GPA_LOW/CREDIT_LOW/ATTENDANCE_LOW) */
    @Excel(name = "规则代码")
    private String ruleCode;

    /** 规则名称 */
    @Excel(name = "规则名称")
    private String ruleName;

    /** 预警类型（0成绩 1学分 2出勤 3综合） */
    @Excel(name = "预警类型", readConverterExp = "0=成绩,1=学分,2=出勤,3=综合")
    private String warningType;

    /** 阈值 */
    @Excel(name = "阈值")
    private String thresholdValue;

    /** 预警级别（0一般 1严重 2高危） */
    @Excel(name = "预警级别", readConverterExp = "0=一般,1=严重,2=高危")
    private String warningLevel;

    /** 消息模板 */
    @Excel(name = "消息模板")
    private String messageTemplate;

    /** 是否启用（0否 1是） */
    @Excel(name = "是否启用", readConverterExp = "0=否,1=是")
    private String isEnabled;

    /** 适用学期ID（空表示所有） */
    @Excel(name = "适用学期ID")
    private Long semesterId;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    @NotBlank(message = "规则代码不能为空")
    @Size(min = 0, max = 50, message = "规则代码长度不能超过50个字符")
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    @NotBlank(message = "规则名称不能为空")
    @Size(min = 0, max = 100, message = "规则名称长度不能超过100个字符")
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }

    public String getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(String thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public String getIsEnabled() { return isEnabled; }
    public void setIsEnabled(String isEnabled) { this.isEnabled = isEnabled; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("ruleCode", getRuleCode())
            .append("ruleName", getRuleName())
            .append("warningType", getWarningType())
            .append("thresholdValue", getThresholdValue())
            .append("warningLevel", getWarningLevel())
            .append("messageTemplate", getMessageTemplate())
            .append("isEnabled", getIsEnabled())
            .append("semesterId", getSemesterId())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
