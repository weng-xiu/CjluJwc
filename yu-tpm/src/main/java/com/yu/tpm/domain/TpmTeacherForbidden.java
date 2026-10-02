package com.yu.tpm.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 教师禁排时间片对象 tpm_teacher_forbidden（F2-1）
 * <p>登记某教师在某学期内不可排课的 (星期×节次窗口)，自动排课引擎将其纳入硬约束屏蔽。</p>
 *
 * @author ruoyi
 * @date 2026-10-02
 */
public class TpmTeacherForbidden extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 禁排ID */
    private Long forbiddenId;

    /** 学期ID（为空表示全学期通用禁排） */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 教师ID */
    @NotNull(message = "教师不能为空")
    @Excel(name = "教师ID")
    private Long teacherId;

    /** 教师姓名（冗余展示，非本表存储列，由关联查询装配） */
    @Excel(name = "教师姓名")
    private String teacherName;

    /** 星期几（1-7） */
    @NotNull(message = "星期不能为空")
    @Excel(name = "星期", readConverterExp = "1=周一,2=周二,3=周三,4=周四,5=周五,6=周六,7=周日")
    private Integer weekDay;

    /** 开始节次 */
    @NotNull(message = "开始节次不能为空")
    @Excel(name = "开始节次")
    private Integer startPeriod;

    /** 结束节次 */
    @NotNull(message = "结束节次不能为空")
    @Excel(name = "结束节次")
    private Integer endPeriod;

    /** 禁排原因 */
    @Excel(name = "禁排原因")
    private String reason;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    public Long getForbiddenId() { return forbiddenId; }
    public void setForbiddenId(Long forbiddenId) { this.forbiddenId = forbiddenId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public Integer getWeekDay() { return weekDay; }
    public void setWeekDay(Integer weekDay) { this.weekDay = weekDay; }

    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }

    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("forbiddenId", getForbiddenId())
            .append("semesterId", getSemesterId())
            .append("teacherId", getTeacherId())
            .append("teacherName", getTeacherName())
            .append("weekDay", getWeekDay())
            .append("startPeriod", getStartPeriod())
            .append("endPeriod", getEndPeriod())
            .append("reason", getReason())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
