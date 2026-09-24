package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 离校环节配置对象 sam_procedure_step（S7）
 * 将原硬编码四环节（图书馆/财务/宿舍/一卡通）升级为可配置环节清单，
 * 并支持按 auto_check_type 从系统内数据源自动判定完成。
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public class SamProcedureStep extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 环节ID */
    private Long stepId;

    /** 环节编码（唯一，LIBRARY/FINANCE/DORMITORY/CARD/CERT_PICKUP/GRAD_REVIEW/DEGREE_REVIEW 或自定义） */
    @Excel(name = "环节编码")
    private String stepKey;

    /** 环节名称 */
    @Excel(name = "环节名称")
    private String stepName;

    /** 排序 */
    @Excel(name = "排序")
    private Integer orderNum;

    /** 是否必办（0否 1是） */
    @Excel(name = "是否必办", readConverterExp = "0=否,1=是")
    private String requiredFlag;

    /** 自动判定数据源（NONE手动登记/CARD一卡通/GRAD_REVIEW毕业审核/DEGREE_REVIEW学位审核/CERT_PICKUP证书发放/LEGACY旧四字段列） */
    @Excel(name = "自动判定", readConverterExp = "NONE=手动登记,CARD=一卡通,GRAD_REVIEW=毕业审核,DEGREE_REVIEW=学位审核,CERT_PICKUP=证书发放,LEGACY=旧字段")
    private String autoCheckType;

    /** 启用状态（0启用 1停用） */
    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    /** 当前完成情况统计（非库字段：已办理该环节完成的学生数） */
    private Integer doneCount;

    public Long getStepId() { return stepId; }
    public void setStepId(Long stepId) { this.stepId = stepId; }

    @NotBlank(message = "环节编码不能为空")
    @Size(min = 0, max = 50, message = "环节编码长度不能超过50个字符")
    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }

    @NotBlank(message = "环节名称不能为空")
    @Size(min = 0, max = 50, message = "环节名称长度不能超过50个字符")
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getRequiredFlag() { return requiredFlag; }
    public void setRequiredFlag(String requiredFlag) { this.requiredFlag = requiredFlag; }

    public String getAutoCheckType() { return autoCheckType; }
    public void setAutoCheckType(String autoCheckType) { this.autoCheckType = autoCheckType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDoneCount() { return doneCount; }
    public void setDoneCount(Integer doneCount) { this.doneCount = doneCount; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("stepId", getStepId())
            .append("stepKey", getStepKey())
            .append("stepName", getStepName())
            .append("orderNum", getOrderNum())
            .append("requiredFlag", getRequiredFlag())
            .append("autoCheckType", getAutoCheckType())
            .append("status", getStatus())
            .append("remark", getRemark())
            .toString();
    }
}
