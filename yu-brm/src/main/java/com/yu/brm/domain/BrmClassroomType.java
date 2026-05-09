package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教室类型对象 brm_classroom_type
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmClassroomType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 类型ID */
    private Long typeId;

    /** 类型名称 */
    @Excel(name = "类型名称")
    private String typeName;

    /** 类型描述 */
    @Excel(name = "类型描述")
    private String typeDesc;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    @NotBlank(message = "类型名称不能为空")
    @Size(min = 0, max = 50, message = "类型名称长度不能超过50个字符")
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    @Size(min = 0, max = 255, message = "类型描述长度不能超过255个字符")
    public String getTypeDesc() { return typeDesc; }
    public void setTypeDesc(String typeDesc) { this.typeDesc = typeDesc; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("typeId", getTypeId())
            .append("typeName", getTypeName())
            .append("typeDesc", getTypeDesc())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
