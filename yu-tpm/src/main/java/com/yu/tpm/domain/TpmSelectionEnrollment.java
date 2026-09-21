package com.yu.tpm.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 选课名单对象 tpm_selection_enrollment
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmSelectionEnrollment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 选课记录ID */
    private Long enrollId;

    /** 轮次ID */
    @Excel(name = "轮次ID")
    private Long roundId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 开课ID */
    @Excel(name = "开课ID")
    private Long courseOfferingId;

    /** 选课时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "选课时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date selectTime;

    /** 抽签结果（0未抽签 1中签 2未中签） */
    @Excel(name = "抽签结果", readConverterExp = "0=未抽签,1=中签,2=未中签")
    private String lotteryResult;

    /** 结果状态（1选中 2落选 3退课） */
    @Excel(name = "结果状态", readConverterExp = "1=选中,2=落选,3=退课")
    private String resultStatus;

    /** T6：候补排名（抽签落选时的递补序号，1为最优；非落选记录为空） */
    @Excel(name = "候补排名")
    private Integer waitlistRank;

    /** 退课时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "退课时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date dropTime;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 学号（关联查询，非持久化） */
    @Excel(name = "学号")
    private String studentCode;

    /** 学生姓名（关联查询，非持久化） */
    @Excel(name = "学生姓名")
    private String studentName;

    /** 课程名称（关联查询，非持久化） */
    @Excel(name = "课程名称")
    private String courseName;

    /** 轮次名称（关联查询，非持久化） */
    @Excel(name = "轮次名称")
    private String roundName;

    public Long getEnrollId() { return enrollId; }
    public void setEnrollId(Long enrollId) { this.enrollId = enrollId; }

    @NotNull(message = "轮次ID不能为空")
    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    @NotNull(message = "开课ID不能为空")
    public Long getCourseOfferingId() { return courseOfferingId; }
    public void setCourseOfferingId(Long courseOfferingId) { this.courseOfferingId = courseOfferingId; }

    public Date getSelectTime() { return selectTime; }
    public void setSelectTime(Date selectTime) { this.selectTime = selectTime; }

    public String getLotteryResult() { return lotteryResult; }
    public void setLotteryResult(String lotteryResult) { this.lotteryResult = lotteryResult; }

    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }

    public Integer getWaitlistRank() { return waitlistRank; }
    public void setWaitlistRank(Integer waitlistRank) { this.waitlistRank = waitlistRank; }

    public Date getDropTime() { return dropTime; }
    public void setDropTime(Date dropTime) { this.dropTime = dropTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getRoundName() { return roundName; }
    public void setRoundName(String roundName) { this.roundName = roundName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("enrollId", getEnrollId())
            .append("roundId", getRoundId())
            .append("studentId", getStudentId())
            .append("courseOfferingId", getCourseOfferingId())
            .append("selectTime", getSelectTime())
            .append("lotteryResult", getLotteryResult())
            .append("resultStatus", getResultStatus())
            .append("dropTime", getDropTime())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
