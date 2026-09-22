package com.yu.tpm.domain.dto;

/**
 * 可排课教室（T1）：状态正常的教室及其楼宇/校区/类型信息，供自动排课引擎做容量、
 * 类型匹配与跨校区软约束评分。
 *
 * @author ruoyi
 */
public class SchedulableClassroom
{
    /** 教室ID */
    private Long classroomId;

    /** 教室名称 */
    private String classroomName;

    /** 所属教学楼ID */
    private Long buildingId;

    /** 教学楼名称 */
    private String buildingName;

    /** 教学楼所属校区ID */
    private Long campusId;

    /** 教室类型ID */
    private Long typeId;

    /** 教室类型名称 */
    private String typeName;

    /** 容纳人数 */
    private Integer capacity;

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
}
