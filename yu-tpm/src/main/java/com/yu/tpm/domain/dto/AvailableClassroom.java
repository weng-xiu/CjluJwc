package com.yu.tpm.domain.dto;

import java.io.Serializable;

/**
 * 可用教室DTO
 *
 * @author ruoyi
 */
public class AvailableClassroom implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 教室ID */
    private Long classroomId;

    /** 教室名称 */
    private String classroomName;

    /** 教学楼名称 */
    private String buildingName;

    /** 容纳人数 */
    private Integer capacity;

    /** 教室类型名称 */
    private String classroomTypeName;

    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getClassroomTypeName() { return classroomTypeName; }
    public void setClassroomTypeName(String classroomTypeName) { this.classroomTypeName = classroomTypeName; }
}
