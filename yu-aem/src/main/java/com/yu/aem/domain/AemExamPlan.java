package com.yu.aem.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 考试安排对象 aem_exam_plan
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public class AemExamPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 考试ID */
    private Long examId;

    /** 考试名称 */
    @Excel(name = "考试名称")
    private String examName;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 考试类型（0期末考试 1补考 2重修考试） */
    @Excel(name = "考试类型", readConverterExp = "0=期末考试,1=补考,2=重修考试")
    private String examType;

    /** 课程ID */
    @Excel(name = "课程ID")
    private Long courseId;

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

    /** 考试时长（分钟） */
    @Excel(name = "考试时长（分钟）")
    private Integer duration;

    /** 考生人数 */
    @Excel(name = "考生人数")
    private Integer totalStudents;

    /** 安排状态（0未安排 1已安排 2已发布） */
    @Excel(name = "安排状态", readConverterExp = "0=未安排,1=已安排,2=已发布")
    private String planStatus;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }

    @NotBlank(message = "考试名称不能为空")
    @Size(min = 0, max = 200, message = "考试名称长度不能超过200个字符")
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    @NotNull(message = "学期ID不能为空")
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotBlank(message = "考试类型不能为空")
    @Size(min = 0, max = 1, message = "考试类型长度不能超过1个字符")
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public String getPlanStatus() { return planStatus; }
    public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("examId", getExamId())
            .append("examName", getExamName())
            .append("semesterId", getSemesterId())
            .append("examType", getExamType())
            .append("courseId", getCourseId())
            .append("examDate", getExamDate())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("duration", getDuration())
            .append("totalStudents", getTotalStudents())
            .append("planStatus", getPlanStatus())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
