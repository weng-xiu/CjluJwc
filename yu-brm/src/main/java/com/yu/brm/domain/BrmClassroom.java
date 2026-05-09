package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教室对象 brm_classroom
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmClassroom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 教室ID */
    private Long classroomId;

    /** 教室名称 */
    @Excel(name = "教室名称")
    private String classroomName;

    /** 所属教学楼ID */
    @Excel(name = "所属教学楼ID")
    private Long buildingId;

    /** 教室类型ID */
    @Excel(name = "教室类型ID")
    private Long typeId;

    /** 容纳人数 */
    @Excel(name = "容纳人数")
    private Integer capacity;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    @NotBlank(message = "教室名称不能为空")
    @Size(min = 0, max = 100, message = "教室名称长度不能超过100个字符")
    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("classroomId", getClassroomId())
            .append("classroomName", getClassroomName())
            .append("buildingId", getBuildingId())
            .append("typeId", getTypeId())
            .append("capacity", getCapacity())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
