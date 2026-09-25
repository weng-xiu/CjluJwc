package com.yu.sam.domain;

import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学业预警帮扶任务对象 sam_warning_assist（S6 帮扶闭环）
 * 一条预警对应一个帮扶任务：派发→认领→跟踪记录→完结/关闭，全过程留痕。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
public class SamWarningAssist extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 帮扶任务ID */
    private Long assistId;

    /** 关联预警ID */
    @Excel(name = "预警ID")
    private Long warningId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 学期ID */
    private Long semesterId;

    /** 学生姓名（冗余展示） */
    @Excel(name = "学生姓名")
    private String studentName;

    /** 学号（冗余展示） */
    @Excel(name = "学号")
    private String studentNo;

    /** 预警类型（0成绩 1学分 2出勤 3综合，冗余自预警） */
    @Excel(name = "预警类型", readConverterExp = "0=成绩预警,1=学分预警,2=出勤预警,3=综合预警")
    private String warningType;

    /** 预警级别（0一般 1严重 2高危，冗余自预警） */
    @Excel(name = "预警级别", readConverterExp = "0=一般,1=严重,2=高危")
    private String warningLevel;

    /** 帮扶人用户ID */
    private Long helperUserId;

    /** 帮扶人姓名 */
    @Excel(name = "帮扶人")
    private String helperName;

    /** 帮扶状态（0待认领 1帮扶中 2已完成 3已关闭） */
    @Excel(name = "帮扶状态", readConverterExp = "0=待认领,1=帮扶中,2=已完成,3=已关闭")
    private String assistStatus;

    /** 首次帮扶措施 */
    private String measure;

    /** 跟踪次数 */
    @Excel(name = "跟踪次数")
    private Integer followCount;

    /** 最近跟踪时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最近跟踪时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastFollowTime;

    /** 认领时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date claimTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "完成时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 完结/关闭说明 */
    private String finishRemark;

    /** 帮扶跟踪记录（详情时装配） */
    private List<SamWarningAssistRecord> records;

    public Long getAssistId() { return assistId; }
    public void setAssistId(Long assistId) { this.assistId = assistId; }

    @NotNull(message = "预警ID不能为空")
    public Long getWarningId() { return warningId; }
    public void setWarningId(Long warningId) { this.warningId = warningId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }

    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }

    public Long getHelperUserId() { return helperUserId; }
    public void setHelperUserId(Long helperUserId) { this.helperUserId = helperUserId; }

    public String getHelperName() { return helperName; }
    public void setHelperName(String helperName) { this.helperName = helperName; }

    public String getAssistStatus() { return assistStatus; }
    public void setAssistStatus(String assistStatus) { this.assistStatus = assistStatus; }

    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }

    public Integer getFollowCount() { return followCount; }
    public void setFollowCount(Integer followCount) { this.followCount = followCount; }

    public Date getLastFollowTime() { return lastFollowTime; }
    public void setLastFollowTime(Date lastFollowTime) { this.lastFollowTime = lastFollowTime; }

    public Date getClaimTime() { return claimTime; }
    public void setClaimTime(Date claimTime) { this.claimTime = claimTime; }

    public Date getFinishTime() { return finishTime; }
    public void setFinishTime(Date finishTime) { this.finishTime = finishTime; }

    public String getFinishRemark() { return finishRemark; }
    public void setFinishRemark(String finishRemark) { this.finishRemark = finishRemark; }

    public List<SamWarningAssistRecord> getRecords() { return records; }
    public void setRecords(List<SamWarningAssistRecord> records) { this.records = records; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("assistId", getAssistId())
            .append("warningId", getWarningId())
            .append("studentId", getStudentId())
            .append("semesterId", getSemesterId())
            .append("studentName", getStudentName())
            .append("studentNo", getStudentNo())
            .append("warningType", getWarningType())
            .append("warningLevel", getWarningLevel())
            .append("helperUserId", getHelperUserId())
            .append("helperName", getHelperName())
            .append("assistStatus", getAssistStatus())
            .append("measure", getMeasure())
            .append("followCount", getFollowCount())
            .append("lastFollowTime", getLastFollowTime())
            .append("claimTime", getClaimTime())
            .append("finishTime", getFinishTime())
            .append("finishRemark", getFinishRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
