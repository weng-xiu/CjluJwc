package com.yu.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.yu.common.annotation.Excel;
import com.yu.common.annotation.Excel.ColumnType;
import com.yu.common.core.domain.BaseEntity;

/**
 * 数据分类分级台账 sys_data_class（K1 等保数据新规 · "从查系统转向查数据"核心抓手）
 *
 * 逐字段登记业务表数据的类别、安全等级与保护措施，形成可审计、可核查的数据资产清单：
 * 等保数据安全系列新规要求组织对数据实施分类分级保护，故台账需覆盖「哪些表/字段、属于什么类别、
 * 定到什么等级、采取了何种保护（脱敏/加密/访问控制）、是否为个人信息」。本台账与运行期的
 * {@code @Sensitive} 脱敏、{@code SensitiveFieldCipher} 字段加密相互印证——登记口径即落地口径。
 *
 * @author yu
 * @date 2026-10-07
 */
public class SysDataClass extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 台账主键 */
    @Excel(name = "编号", cellType = ColumnType.NUMERIC)
    private Long classId;

    /** 业务表名 */
    @Excel(name = "表名")
    private String tableName;

    /** 表中文名 */
    @Excel(name = "表中文名")
    private String tableComment;

    /** 字段名 */
    @Excel(name = "字段名")
    private String columnName;

    /** 字段中文名 */
    @Excel(name = "字段中文名")
    private String columnComment;

    /** 数据类别（字典 sys_data_category：PII个人身份/CONTACT联系方式/ACADEMIC学业/FINANCIAL财务/OPERATION运行/OTHER其他） */
    @Excel(name = "数据类别", dictType = "sys_data_category")
    private String dataCategory;

    /** 安全等级（字典 sys_data_security_level：L1公开/L2内部/L3敏感/L4核心敏感） */
    @Excel(name = "安全等级", dictType = "sys_data_security_level")
    private String securityLevel;

    /** 保护措施（字典 sys_data_protection：NONE无/MASK脱敏/ENCRYPT加密/ACCESS访问控制/MASK_ENCRYPT脱敏且加密） */
    @Excel(name = "保护措施", dictType = "sys_data_protection")
    private String protectionMeasure;

    /** 是否个人信息（Y是 N否） */
    @Excel(name = "个人信息", readConverterExp = "Y=是,N=否")
    private String isPersonalInfo;

    /** 状态（0正常 1停用登记） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getClassId()
    {
        return classId;
    }

    public void setClassId(Long classId)
    {
        this.classId = classId;
    }

    @NotBlank(message = "表名不能为空")
    @Size(min = 0, max = 64, message = "表名不能超过64个字符")
    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    public String getTableComment()
    {
        return tableComment;
    }

    public void setTableComment(String tableComment)
    {
        this.tableComment = tableComment;
    }

    @NotBlank(message = "字段名不能为空")
    @Size(min = 0, max = 64, message = "字段名不能超过64个字符")
    public String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }

    public String getColumnComment()
    {
        return columnComment;
    }

    public void setColumnComment(String columnComment)
    {
        this.columnComment = columnComment;
    }

    @NotBlank(message = "数据类别不能为空")
    public String getDataCategory()
    {
        return dataCategory;
    }

    public void setDataCategory(String dataCategory)
    {
        this.dataCategory = dataCategory;
    }

    @NotBlank(message = "安全等级不能为空")
    public String getSecurityLevel()
    {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel)
    {
        this.securityLevel = securityLevel;
    }

    public String getProtectionMeasure()
    {
        return protectionMeasure;
    }

    public void setProtectionMeasure(String protectionMeasure)
    {
        this.protectionMeasure = protectionMeasure;
    }

    public String getIsPersonalInfo()
    {
        return isPersonalInfo;
    }

    public void setIsPersonalInfo(String isPersonalInfo)
    {
        this.isPersonalInfo = isPersonalInfo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("classId", getClassId())
                .append("tableName", getTableName())
                .append("tableComment", getTableComment())
                .append("columnName", getColumnName())
                .append("columnComment", getColumnComment())
                .append("dataCategory", getDataCategory())
                .append("securityLevel", getSecurityLevel())
                .append("protectionMeasure", getProtectionMeasure())
                .append("isPersonalInfo", getIsPersonalInfo())
                .append("status", getStatus())
                .append("remark", getRemark())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
