package com.yu.sam.domain;

import java.util.Date;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * OBE持续改进台账对象 sam_obe_improvement（F3 整改闭环）
 *
 * @author N1
 */
public class SamObeImprovement extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 改进台账ID */
    private Long impId;

    /** 培养方案ID */
    @Excel(name = "培养方案ID")
    private Long planId;

    /** 学期ID */
    private Long semesterId;

    /** 毕业要求ID */
    @Excel(name = "毕业要求ID")
    private Long grId;

    /** 指标点ID（可空=整条毕业要求） */
    private Long ipId;

    /** 发现周期 */
    @Excel(name = "发现周期")
    private String sourceTerm;

    /** 发现时达成度 */
    @Excel(name = "发现时达成度")
    private Double attainment;

    /** 发现时目标值 */
    @Excel(name = "目标值")
    private Double targetValue;

    /** 问题分析 */
    private String problemDesc;

    /** 改进措施 */
    @Excel(name = "改进措施")
    private String measure;

    /** 责任人 */
    @Excel(name = "责任人")
    private String owner;

    /** 计划完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "计划完成日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dueDate;

    /** 状态（0进行中 1已完成 2已验证） */
    @Excel(name = "状态", readConverterExp = "0=进行中,1=已完成,2=已验证")
    private String status;

    /** 效果反馈：改进后达成度 */
    @Excel(name = "改进后达成度")
    private Double effectAttainment;

    /** 效果反馈说明 */
    private String effectNote;

    /** 删除标志 */
    private String delFlag;

    /** 毕业要求编号（联查展示，非表字段） */
    @Excel(name = "要求编号")
    private String grCode;

    /** 毕业要求名称（联查展示，非表字段） */
    @Excel(name = "毕业要求")
    private String grName;

    public Long getImpId() { return impId; }
    public void setImpId(Long impId) { this.impId = impId; }

    @NotNull(message = "培养方案ID不能为空")
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public Long getGrId() { return grId; }
    public void setGrId(Long grId) { this.grId = grId; }

    public Long getIpId() { return ipId; }
    public void setIpId(Long ipId) { this.ipId = ipId; }

    public String getSourceTerm() { return sourceTerm; }
    public void setSourceTerm(String sourceTerm) { this.sourceTerm = sourceTerm; }

    public Double getAttainment() { return attainment; }
    public void setAttainment(Double attainment) { this.attainment = attainment; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public String getProblemDesc() { return problemDesc; }
    public void setProblemDesc(String problemDesc) { this.problemDesc = problemDesc; }

    @NotNull(message = "改进措施不能为空")
    @Size(min = 0, max = 500, message = "改进措施长度不能超过500个字符")
    public String getMeasure() { return measure; }
    public void setMeasure(String measure) { this.measure = measure; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getEffectAttainment() { return effectAttainment; }
    public void setEffectAttainment(Double effectAttainment) { this.effectAttainment = effectAttainment; }

    public String getEffectNote() { return effectNote; }
    public void setEffectNote(String effectNote) { this.effectNote = effectNote; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getGrCode() { return grCode; }
    public void setGrCode(String grCode) { this.grCode = grCode; }

    public String getGrName() { return grName; }
    public void setGrName(String grName) { this.grName = grName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("impId", getImpId())
            .append("planId", getPlanId())
            .append("semesterId", getSemesterId())
            .append("grId", getGrId())
            .append("ipId", getIpId())
            .append("sourceTerm", getSourceTerm())
            .append("attainment", getAttainment())
            .append("targetValue", getTargetValue())
            .append("problemDesc", getProblemDesc())
            .append("measure", getMeasure())
            .append("owner", getOwner())
            .append("dueDate", getDueDate())
            .append("status", getStatus())
            .append("effectAttainment", getEffectAttainment())
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
