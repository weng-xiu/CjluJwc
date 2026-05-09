package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教学楼对象 brm_building
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmBuilding extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教学楼ID */
    private Long buildingId;

    /** 教学楼名称 */
    @Excel(name = "教学楼名称")
    private String buildingName;

    /** 教学楼编码 */
    @Excel(name = "教学楼编码")
    private String buildingCode;

    /** 所属校区ID */
    @Excel(name = "所属校区ID")
    private Long campusId;

    /** 楼层数 */
    @Excel(name = "楼层数")
    private Integer floorCount;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }

    @NotBlank(message = "教学楼名称不能为空")
    @Size(min = 0, max = 100, message = "教学楼名称长度不能超过100个字符")
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getBuildingCode() { return buildingCode; }
    public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Integer getFloorCount() { return floorCount; }
    public void setFloorCount(Integer floorCount) { this.floorCount = floorCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("buildingId", getBuildingId())
            .append("buildingName", getBuildingName())
            .append("buildingCode", getBuildingCode())
            .append("campusId", getCampusId())
            .append("floorCount", getFloorCount())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
