package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 会议室对象 oa_meeting_room
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaMeetingRoom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 会议室ID */
    private Long roomId;

    /** 会议室名称 */
    @Excel(name = "会议室名称")
    private String roomName;

    /** 会议室位置 */
    @Excel(name = "会议室位置")
    private String roomLocation;

    /** 容纳人数 */
    @Excel(name = "容纳人数")
    private Integer capacity;

    /** 设备配置 */
    @Excel(name = "设备配置")
    private String equipment;

    /** 管理员用户ID */
    @Excel(name = "管理员用户ID")
    private Long adminId;

    /** 管理员姓名 */
    @Excel(name = "管理员姓名")
    private String adminName;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomLocation() { return roomLocation; }
    public void setRoomLocation(String roomLocation) { this.roomLocation = roomLocation; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roomId", getRoomId())
            .append("roomName", getRoomName())
            .append("roomLocation", getRoomLocation())
            .append("capacity", getCapacity())
            .append("equipment", getEquipment())
            .append("adminId", getAdminId())
            .append("adminName", getAdminName())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
