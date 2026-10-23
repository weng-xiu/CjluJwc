package com.yu.sam.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学生干预闭环记录对象 sam_student_intervention（F4 预警-帮扶-效果回收）
 *
 * 效果指标（GPA/及格率）不落库，由基线学期与效果学期口径在 aem_grade_record 现算，
 * 避免成绩补录后统计值失真。
 *
 * @author F4
 */
public class SamStudentIntervention extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 干预记录ID */
    private Long interventionId;

    /** 学生ID */
    private Long studentId;

    /** 预警类型（0学业预警 1成绩预警 2出勤预警 3其他） */
    private String warningType;

    /** 干预类型（0学业辅导 1心理疏导 2家校联系 3课程重修 4学习计划 5其他） */
    @Excel(name = "干预类型", readConverterExp = "0=学业辅导,1=心理疏导,2=家校联系,3=课程重修,4=学习计划,5=其他")
    private String interventionType;

    /** 干预措施 */
    @Excel(name = "干预措施")
    private String measure;

    /** 帮扶人 */
    @Excel(name = "帮扶人")
    private String helperName;

    /** 基线学期ID（干预前） */
    private Long baselineSemesterId;

    /** 效果考察学期ID */
    private Long effectSemesterId;

    /** 干预开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startDate;

    /** 干预结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endDate;

    /** 状态（0进行中 1已完成 2已关闭） */
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已完成,2=已关闭")
    private String status;

    /** 效果结论（定性描述） */
    @Excel(name = "效果结论")
    private String effectNote;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 学号（联查展示，非表字段） */
    @Excel(name = "学号")
    private String studentNo;

    /** 姓名（联查展示，非表字段） */
    @Excel(name = "姓名")
    private String studentName;

    /** 基线学期名称（联查展示，非表字段） */
    @Excel(name = "基线学期")
    private String baselineSemesterName;

    /** 效果学期名称（联查展示，非表字段） */
    @Excel(name = "效果学期")
    private String effectSemesterName;

    /** 干预前 GPA（现算，非表字段） */
    @Excel(name = "干预前GPA")
    private Double baselineGpa;

    /** 干预后 GPA（现算，非表字段） */
    @Excel(name = "干预后GPA")
    private Double effectGpa;

    /** 干预前及格率%（现算，非表字段） */
    @Excel(name = "干预前及格率%")
    private Double baselinePassRate;

    /** 干预后及格率%（现算，非表字段） */
    @Excel(name = "干预后及格率%")
    private Double effectPassRate;

    /** GPA 变化（效果-基线，现算，非表字段） */
    @Excel(name = "GPA变化")
    private Double gpaDelta;

    /** 及格率变化百分点（效果-基线，现算，非表字段） */
    @Excel(name = "及格率变化pp")
    private Double passRateDelta;

    public Long getInterventionId() { return interventionId; }
    public void setInterventionId(Long interventionId) { this.interventionId = interventionId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }

    @NotBlank(message = "干预类型不能为空")
    public String getInterventionType() { return interventionType; }
    public void setInterventionType(String interventionType) { this.interventionType = interventionType; }

    @NotBlank(message = "干预措施不能为空")
    @Size(min = 0, max = 500, message = "干预措施长度不能超过500个字符")
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }

    public String getHelperName() { return helperName; }
    public void setHelperName(String helperName) { this.helperName = helperName; }

    public Long getBaselineSemesterId() { return baselineSemesterId; }
    public void setBaselineSemesterId(Long baselineSemesterId) { this.baselineSemesterId = baselineSemesterId; }

    public Long getEffectSemesterId() { return effectSemesterId; }
    public void setEffectSemesterId(Long effectSemesterId) { this.effectSemesterId = effectSemesterId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEffectNote() { return effectNote; }
    public void setEffectNote(String effectNote) { this.effectNote = effectNote; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getBaselineSemesterName() { return baselineSemesterName; }
    public void setBaselineSemesterName(String baselineSemesterName) { this.baselineSemesterName = baselineSemesterName; }

    public String getEffectSemesterName() { return effectSemesterName; }
    public void setEffectSemesterName(String effectSemesterName) { this.effectSemesterName = effectSemesterName; }

    public Double getBaselineGpa() { return baselineGpa; }
    public void setBaselineGpa(Double baselineGpa) { this.baselineGpa = baselineGpa; }

    public Double getEffectGpa() { return effectGpa; }
    public void setEffectGpa(Double effectGpa) { this.effectGpa = effectGpa; }

    public Double getBaselinePassRate() { return baselinePassRate; }
    public void setBaselinePassRate(Double baselinePassRate) { this.baselinePassRate = baselinePassRate; }

    public Double getEffectPassRate() { return effectPassRate; }
    public void setEffectPassRate(Double effectPassRate) { this.effectPassRate = effectPassRate; }

    public Double getGpaDelta() { return gpaDelta; }
    public void setGpaDelta(Double gpaDelta) { this.gpaDelta = gpaDelta; }

    public Double getPassRateDelta() { return passRateDelta; }
    public void setPassRateDelta(Double passRateDelta) { this.passRateDelta = passRateDelta; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("interventionId", getInterventionId())
            .append("studentId", getStudentId())
            .append("warningType", getWarningType())
            .append("interventionType", getInterventionType())
            .append("measure", getMeasure())
            .append("helperName", getHelperName())
            .append("baselineSemesterId", getBaselineSemesterId())
            .append("effectSemesterId", getEffectSemesterId())
            .append("startDate", getStartDate())
            .append("endDate", getEndDate())
            .append("status", getStatus())
            .append("effectNote", getEffectNote())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
