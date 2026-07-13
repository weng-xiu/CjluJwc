package com.yu.portal.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 门户栏目对象 portal_column
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public class PortalColumn extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 栏目ID */
    private Long columnId;

    /** 栏目名称 */
    @Excel(name = "栏目名称")
    private String columnName;

    /** 栏目编码 */
    @Excel(name = "栏目编码")
    private String columnCode;

    /** 父栏目ID */
    @Excel(name = "父栏目ID")
    private Long parentId;

    /** 栏目类型(1列表 2单页 3链接) */
    @Excel(name = "栏目类型", readConverterExp = "1=列表,2=单页,3=链接")
    private String columnType;

    /** 图标 */
    @Excel(name = "图标")
    private String icon;

    /** 显示排序 */
    @Excel(name = "显示排序")
    private Integer sortOrder;

    /** 是否显示(0否 1是) */
    @Excel(name = "是否显示", readConverterExp = "0=否,1=是")
    private String isVisible;

    /** 外部链接(type=3时使用) */
    @Excel(name = "外部链接")
    private String externalUrl;

    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }

    @NotBlank(message = "栏目名称不能为空")
    @Size(min = 0, max = 100, message = "栏目名称长度不能超过100个字符")
    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    @NotBlank(message = "栏目编码不能为空")
    @Size(min = 0, max = 50, message = "栏目编码长度不能超过50个字符")
    public String getColumnCode() { return columnCode; }
    public void setColumnCode(String columnCode) { this.columnCode = columnCode; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getColumnType() { return columnType; }
    public void setColumnType(String columnType) { this.columnType = columnType; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getIsVisible() { return isVisible; }
    public void setIsVisible(String isVisible) { this.isVisible = isVisible; }

    @Size(min = 0, max = 500, message = "外部链接长度不能超过500个字符")
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("columnId", getColumnId())
            .append("columnName", getColumnName())
            .append("columnCode", getColumnCode())
            .append("parentId", getParentId())
            .append("columnType", getColumnType())
            .append("icon", getIcon())
            .append("sortOrder", getSortOrder())
            .append("isVisible", getIsVisible())
            .append("externalUrl", getExternalUrl())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
