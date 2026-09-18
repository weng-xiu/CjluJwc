package com.yu.aem.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 监考分配对象 aem_exam_invigilation
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemExamInvigilation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 监考ID */
    private Long invigilationId;

    /** 考试ID */
    @Excel(name = "考试ID")
    private Long examId;

    /** 教室ID */
    @Excel(name = "教室ID")
    private Long classroomId;

    /** 监考教师ID */
    @Excel(name = "监考教师ID")
    private Long teacherId;

    /** 考试日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "考试日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date examDate;

    /** 开始时间 */
    @Excel(name = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Excel(name = "结束时间")
    private String endTime;

    /** 职责（0主监考 1副监考 2巡考） */
    @Excel(name = "职责", readConverterExp = "0=主监考,1=副监考,2=巡考")
    private String dutyType;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 考试名称（关联查询，门户移动端展示用） */
    private String examName;

    /** 教室名称（关联查询，门户移动端展示用） */
    private String classroomName;

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getClassroomName() { return classroomName; }
    public void setClassroomName(String classroomName) { this.classroomName = classroomName; }

    public Long getInvigilationId() { return invigilationId; }
    public void setInvigilationId(Long invigilationId) { this.invigilationId = invigilationId; }

    @NotNull(message = "考试ID不能为空")
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    @NotNull(message = "教室ID不能为空")
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }

    @NotNull(message = "监考教师不能为空")
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getDutyType() { return dutyType; }
    public void setDutyType(String dutyType) { this.dutyType = dutyType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("invigilationId", getInvigilationId())
            .append("examId", getExamId())
            .append("classroomId", getClassroomId())
            .append("teacherId", getTeacherId())
            .append("examDate", getExamDate())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("dutyType", getDutyType())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
