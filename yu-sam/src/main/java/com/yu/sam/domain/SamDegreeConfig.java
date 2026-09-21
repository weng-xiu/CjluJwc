package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学位授予条件配置对象 sam_degree_config
 *
 * <p>S3：学位审核条件后台可配置（GPA、学位课程、外语、论文、学术成果），
 * 替代旧实现中硬编码的 GPA≥2.0 与论文恒合格。</p>
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public class SamDegreeConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 配置ID */
    private Long configId;

    /** 配置名称 */
    @Excel(name = "配置名称")
    private String configName;

    /** GPA最低要求 */
    @Excel(name = "GPA最低要求")
    private Double gpaThreshold;

    /** 学位课程是否须全部合格（0否 1是） */
    @Excel(name = "学位课程须合格", readConverterExp = "0=否,1=是")
    private String requireDegreeCourse;

    /** 外语课程是否须无不及格（0否 1是） */
    @Excel(name = "外语须合格", readConverterExp = "0=否,1=是")
    private String requireForeignLanguage;

    /** 是否要求论文合格（0否=无数据默认放行 1是=无论文数据时判定不合格并提示对接） */
    @Excel(name = "要求论文", readConverterExp = "0=否,1=是")
    private String requireThesis;

    /** 是否要求学术成果（0否 1是，无数据源时需人工确认） */
    @Excel(name = "要求学术成果", readConverterExp = "0=否,1=是")
    private String requireAchievement;

    /** 是否默认配置（0否 1是） */
    @Excel(name = "是否默认", readConverterExp = "0=否,1=是")
    private String isDefault;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }

    @NotBlank(message = "配置名称不能为空")
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public Double getGpaThreshold() { return gpaThreshold; }
    public void setGpaThreshold(Double gpaThreshold) { this.gpaThreshold = gpaThreshold; }

    public String getRequireDegreeCourse() { return requireDegreeCourse; }
    public void setRequireDegreeCourse(String requireDegreeCourse) { this.requireDegreeCourse = requireDegreeCourse; }

    public String getRequireForeignLanguage() { return requireForeignLanguage; }
    public void setRequireForeignLanguage(String requireForeignLanguage) { this.requireForeignLanguage = requireForeignLanguage; }

    public String getRequireThesis() { return requireThesis; }
    public void setRequireThesis(String requireThesis) { this.requireThesis = requireThesis; }

    public String getRequireAchievement() { return requireAchievement; }
    public void setRequireAchievement(String requireAchievement) { this.requireAchievement = requireAchievement; }

    public String getIsDefault() { return isDefault; }
    public void setIsDefault(String isDefault) { this.isDefault = isDefault; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configName", getConfigName())
            .append("gpaThreshold", getGpaThreshold())
            .append("requireDegreeCourse", getRequireDegreeCourse())
            .append("requireForeignLanguage", getRequireForeignLanguage())
            .append("requireThesis", getRequireThesis())
            .append("requireAchievement", getRequireAchievement())
            .append("isDefault", getIsDefault())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
