package com.yu.brm.domain;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 设备维护记录对象 brm_equipment_maintenance
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmEquipmentMaintenance extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 维护ID */
    private Long maintenanceId;

    /** 设备ID */
    private Long equipId;

    /** 故障描述 */
    @Excel(name = "故障描述")
    private String faultDesc;

    /** 维修日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "维修日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date repairDate;

    /** 维修人 */
    @Excel(name = "维修人")
    private String repairBy;

    /** 维修费用 */
    @Excel(name = "维修费用")
    private BigDecimal repairCost;

    /** 维修结果 */
    @Excel(name = "维修结果")
    private String result;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(Long maintenanceId) { this.maintenanceId = maintenanceId; }

    public Long getEquipId() { return equipId; }
    public void setEquipId(Long equipId) { this.equipId = equipId; }

    @NotBlank(message = "故障描述不能为空")
    @Size(min = 0, max = 500, message = "故障描述长度不能超过500个字符")
    public String getFaultDesc() { return faultDesc; }
    public void setFaultDesc(String faultDesc) { this.faultDesc = faultDesc; }

    public Date getRepairDate() { return repairDate; }
    public void setRepairDate(Date repairDate) { this.repairDate = repairDate; }

    public String getRepairBy() { return repairBy; }
    public void setRepairBy(String repairBy) { this.repairBy = repairBy; }

    public BigDecimal getRepairCost() { return repairCost; }
    public void setRepairCost(BigDecimal repairCost) { this.repairCost = repairCost; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("maintenanceId", getMaintenanceId())
            .append("equipId", getEquipId())
            .append("faultDesc", getFaultDesc())
            .append("repairDate", getRepairDate())
            .append("repairBy", getRepairBy())
            .append("repairCost", getRepairCost())
            .append("result", getResult())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
