package com.yu.sam.domain;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 毕业离校手续对象 sam_graduation_procedure
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public class SamGraduationProcedure extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 手续ID */
    private Long procedureId;

    /** 学生ID */
    @Excel(name = "学生ID")
    private Long studentId;

    /** 图书馆清还（0未清 1已清） */
    @Excel(name = "图书馆清还", readConverterExp = "0=未清,1=已清")
    private String libraryCleared;

    /** 财务结算（0未结 1已结） */
    @Excel(name = "财务结算", readConverterExp = "0=未结,1=已结")
    private String financeCleared;

    /** 宿舍退宿（0未退 1已退） */
    @Excel(name = "宿舍退宿", readConverterExp = "0=未退,1=已退")
    private String dormitoryCleared;

    /** 一卡通退还（0未退 1已退） */
    @Excel(name = "一卡通退还", readConverterExp = "0=未退,1=已退")
    private String cardReturned;

    /** 手续状态（0未办理 1办理中 2已完成） */
    @Excel(name = "手续状态", readConverterExp = "0=未办理,1=办理中,2=已完成")
    private String procedureStatus;

    /** 完成日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "完成日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date completeDate;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getProcedureId() { return procedureId; }
    public void setProcedureId(Long procedureId) { this.procedureId = procedureId; }

    @NotNull(message = "学生ID不能为空")
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getLibraryCleared() { return libraryCleared; }
    public void setLibraryCleared(String libraryCleared) { this.libraryCleared = libraryCleared; }

    public String getFinanceCleared() { return financeCleared; }
    public void setFinanceCleared(String financeCleared) { this.financeCleared = financeCleared; }

    public String getDormitoryCleared() { return dormitoryCleared; }
    public void setDormitoryCleared(String dormitoryCleared) { this.dormitoryCleared = dormitoryCleared; }

    public String getCardReturned() { return cardReturned; }
    public void setCardReturned(String cardReturned) { this.cardReturned = cardReturned; }

    public String getProcedureStatus() { return procedureStatus; }
    public void setProcedureStatus(String procedureStatus) { this.procedureStatus = procedureStatus; }

    public Date getCompleteDate() { return completeDate; }
    public void setCompleteDate(Date completeDate) { this.completeDate = completeDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("procedureId", getProcedureId())
            .append("studentId", getStudentId())
            .append("libraryCleared", getLibraryCleared())
            .append("financeCleared", getFinanceCleared())
            .append("dormitoryCleared", getDormitoryCleared())
            .append("cardReturned", getCardReturned())
            .append("procedureStatus", getProcedureStatus())
            .append("completeDate", getCompleteDate())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
