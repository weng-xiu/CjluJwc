package com.yu.tpm.domain.dto;

/**
 * 批量生成开课计划请求（T4）。
 *
 * 依据培养方案（课程库按 planId 关联）一键生成某学期的开课计划，
 * 支持容量与教师预分配。生成结果默认为"待确认(0)"状态，可编辑后再确认。
 *
 * @author ruoyi
 */
public class BatchOfferingRequest
{
    /** 培养方案ID（必填） */
    private Long planId;

    /** 学期ID（必填） */
    private Long semesterId;

    /** 建议修读学期序号（可选，用于仅生成该学期课程；为空则生成方案全部课程） */
    private Integer semesterOrder;

    /** 校区ID（可选） */
    private Long campusId;

    /** 教师预分配院系列（可选，限定教师池；为空则使用全部在职教师） */
    private Long teacherDeptId;

    /** 默认容量（可选，为空取系统配置 tpm.schedule.defaultCapacity 或 30） */
    private Integer defaultCapacity;

    /** 每门课教学班数（可选，为空默认 1） */
    private Integer classCount;

    /** 是否跳过已存在开课的课程（默认为真，保证幂等，不产生重复开课） */
    private Boolean skipExisting = Boolean.TRUE;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public Integer getSemesterOrder() { return semesterOrder; }
    public void setSemesterOrder(Integer semesterOrder) { this.semesterOrder = semesterOrder; }

    public Long getCampusId() { return campusId; }
    public void setCampusId(Long campusId) { this.campusId = campusId; }

    public Long getTeacherDeptId() { return teacherDeptId; }
    public void setTeacherDeptId(Long teacherDeptId) { this.teacherDeptId = teacherDeptId; }

    public Integer getDefaultCapacity() { return defaultCapacity; }
    public void setDefaultCapacity(Integer defaultCapacity) { this.defaultCapacity = defaultCapacity; }

    public Integer getClassCount() { return classCount; }
    public void setClassCount(Integer classCount) { this.classCount = classCount; }

    public Boolean getSkipExisting() { return skipExisting; }
    public void setSkipExisting(Boolean skipExisting) { this.skipExisting = skipExisting; }
}
