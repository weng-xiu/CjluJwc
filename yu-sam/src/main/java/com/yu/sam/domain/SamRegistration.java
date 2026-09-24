package com.yu.sam.domain;

import java.util.Date;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学期注册记录对象 sam_registration（S8 学期注册与报到管理）
 *
 * @author ruoyi
 * @date 2026-09-23
 */
public class SamRegistration extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 注册记录ID */
    private Long registrationId;

    /** 学生ID */
    private Long studentId;

    /** 学期ID */
    @NotNull(message = "学期不能为空")
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 注册状态（0未注册 1已注册 2延迟注册） */
    @Excel(name = "注册状态", readConverterExp = "0=未注册,1=已注册,2=延迟注册")
    private String registerStatus;

    /** 报到注册时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "注册时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date registerTime;

    /** 注册经办人 */
    @Excel(name = "经办人")
    private String registerBy;

    /** 注册渠道（0管理端代办 1学生自助） */
    @Excel(name = "渠道", readConverterExp = "0=管理端,1=学生自助")
    private String channel;

    /** 延迟/未注册原因 */
    @Excel(name = "延迟原因")
    private String deferReason;

    // ===== 关联展示字段（来自 join，非本表列） =====
    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 姓名 */
    @Excel(name = "姓名")
    private String studentName;

    /** 班级名称 */
    @Excel(name = "班级")
    private String className;

    /** 院系名称 */
    @Excel(name = "院系")
    private String deptName;

    /** 学期名称 */
    @Excel(name = "学期")
    private String semesterName;

    /** 学籍状态（用于筛选在读学生） */
    private String studentStatus;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getRegisterStatus() { return registerStatus; }
    public void setRegisterStatus(String registerStatus) { this.registerStatus = registerStatus; }

    public Date getRegisterTime() { return registerTime; }
    public void setRegisterTime(Date registerTime) { this.registerTime = registerTime; }

    public String getRegisterBy() { return registerBy; }
    public void setRegisterBy(String registerBy) { this.registerBy = registerBy; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getDeferReason() { return deferReason; }
    public void setDeferReason(String deferReason) { this.deferReason = deferReason; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("registrationId", getRegistrationId())
            .append("studentId", getStudentId())
            .append("semesterId", getSemesterId())
            .append("registerStatus", getRegisterStatus())
            .append("registerTime", getRegisterTime())
            .append("registerBy", getRegisterBy())
            .append("channel", getChannel())
            .append("deferReason", getDeferReason())
            .append("studentNo", getStudentNo())
            .append("studentName", getStudentName())
            .append("className", getClassName())
            .append("deptName", getDeptName())
            .append("semesterName", getSemesterName())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
