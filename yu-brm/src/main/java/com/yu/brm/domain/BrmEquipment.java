package com.yu.brm.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 多媒体设备对象 brm_equipment
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmEquipment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long equipId;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String equipName;

    /** 所属教室ID */
    @Excel(name = "所属教室ID")
    private Long classroomId;

    /** 设备类型 */
    @Excel(name = "设备类型")
    private String equipType;

    /** 设备型号 */
    @Excel(name = "设备型号")
    private String model;

    /** 购置日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "购置日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date purchaseDate;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getEquipId() { return equipId; }
    public void setEquipId(Long equipId) { this.equipId = equipId; }

    @NotBlank(message = "设备名称不能为空")
    @Size(min = 0, max = 100, message = "设备名称长度不能超过100个字符")
    public String getEquipName() { return equipName; }
    public void setEquipName(String equipName) { this.equipName = equipName; }

    @NotNull(message = "所属教室不能为空")
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    @NotBlank(message = "设备类型不能为空")
    @Size(min = 0, max = 50, message = "设备类型长度不能超过50个字符")
    public String getEquipType() { return equipType; }
    public void setEquipType(String equipType) { this.equipType = equipType; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("equipId", getEquipId())
            .append("equipName", getEquipName())
            .append("classroomId", getClassroomId())
            .append("equipType", getEquipType())
            .append("model", getModel())
            .append("purchaseDate", getPurchaseDate())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
