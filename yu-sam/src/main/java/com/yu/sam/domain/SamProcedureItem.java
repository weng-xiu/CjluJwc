package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 学生离校环节办理明细对象 sam_procedure_item（S7）
 * 与可配置环节（sam_procedure_step）联动，取代原硬编码四列的登记方式；
 * 原四列仍可通过 LEGACY 数据源自动同步到本表。
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public class SamProcedureItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    private Long itemId;

    /** 离校手续ID */
    private Long procedureId;

    /** 学生ID */
    @NotNull(message = "学生ID不能为空")
    private Long studentId;

    /** 环节ID */
    @NotNull(message = "环节不能为空")
    private Long stepId;

    /** 环节编码（联查字段） */
    private String stepKey;

    /** 环节名称（联查字段） */
    private String stepName;

    /** 是否必办（联查字段） */
    private String requiredFlag;

    /** 办理状态（0未办 1已办） */
    private String itemStatus;

    /** 判定方式（0人工登记 1自动判定） */
    private String checkType;

    /** 办理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkTime;

    /** 办理人/数据源 */
    private String checkBy;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getProcedureId() { return procedureId; }
    public void setProcedureId(Long procedureId) { this.procedureId = procedureId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(String requiredFlag) { this.requiredFlag = requiredFlag; }

    public String getItemStatus() { return itemStatus; }
    public void setItemStatus(String itemStatus) { this.itemStatus = itemStatus; }

    public String getCheckType() { return checkType; }
    public void setCheckType(String checkType) { this.checkType = checkType; }

    public Date getCheckTime() { return checkTime; }
    public void setCheckTime(Date checkTime) { this.checkTime = checkTime; }

    public String getCheckBy() { return checkBy; }
    public void setCheckBy(String checkBy) { this.checkBy = checkBy; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("procedureId", getProcedureId())
            .append("studentId", getStudentId())
            .append("stepId", getStepId())
            .append("stepKey", getStepKey())
            .append("stepName", getStepName())
            .append("itemStatus", getItemStatus())
            .append("checkType", getCheckType())
            .append("checkTime", getCheckTime())
            .append("checkBy", getCheckBy())
            .toString();
    }
}
