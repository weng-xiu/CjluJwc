package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 校区对象 brm_campus
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmCampus extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 校区ID */
    private Long campusId;

    /** 校区名称 */
    @Excel(name = "校区名称")
    private String campusName;

    /** 校区地址 */
    @Excel(name = "校区地址")
    private String campusAddress;

    /** 显示顺序 */
    @Excel(name = "显示顺序")
    private Integer orderNum;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    @NotBlank(message = "校区名称不能为空")
    @Size(min = 0, max = 100, message = "校区名称长度不能超过100个字符")
    public String getCampusName() { return campusName; }
    public void setCampusName(String campusName) { this.campusName = campusName; }

    @Size(min = 0, max = 255, message = "校区地址长度不能超过255个字符")
    public String getCampusAddress() { return campusAddress; }
    public void setCampusAddress(String campusAddress) { this.campusAddress = campusAddress; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("campusId", getCampusId())
            .append("campusName", getCampusName())
            .append("campusAddress", getCampusAddress())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
