package com.yu.web.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 状态数据上报批次对象 sys_status_report_batch
 *
 * <p>每次「一键生成」形成一个批次，记录类型、年度、范围与行数；
 * 导出与上报标记按批次流转状态留痕，便于重复上报排查。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusReportBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 批次ID */
    private Long batchId;

    /** 上报类型（01学生 02课程 03成绩 04教师） */
    @Excel(name = "上报类型", readConverterExp = "01=学生基本信息,02=课程基本信息,03=成绩信息,04=教师基本信息")
    private String reportType;

    /** 上报年度 */
    @Excel(name = "上报年度")
    private String reportYear;

    /** 批次名称 */
    @Excel(name = "批次名称")
    private String reportName;

    /** 统计范围院系ID（0表示全校） */
    private Long scopeDeptId;

    /** 统计范围院系名称 */
    @Excel(name = "上报范围")
    private String scopeDeptName;

    /** 生成数据行数 */
    @Excel(name = "数据行数")
    private Integer rowCount;

    /** 批次状态（0已生成 1已导出 2已上报 3已作废） */
    @Excel(name = "批次状态", readConverterExp = "0=已生成,1=已导出,2=已上报,3=已作废")
    private String batchStatus;

    /** 生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "生成时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date genTime;

    /** 最近导出时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date exportTime;

    /** 上报标记时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getReportYear() { return reportYear; }
    public void setReportYear(String reportYear) { this.reportYear = reportYear; }
    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }
    public Long getScopeDeptId() { return scopeDeptId; }
    public void setScopeDeptId(Long scopeDeptId) { this.scopeDeptId = scopeDeptId; }
    public String getScopeDeptName() { return scopeDeptName; }
    public void setScopeDeptName(String scopeDeptName) { this.scopeDeptName = scopeDeptName; }
    public Integer getRowCount() { return rowCount; }
    public void setRowCount(Integer rowCount) { this.rowCount = rowCount; }
    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }
    public Date getGenTime() { return genTime; }
    public void setGenTime(Date genTime) { this.genTime = genTime; }
    public Date getExportTime() { return exportTime; }
    public void setExportTime(Date exportTime) { this.exportTime = exportTime; }
    public Date getSubmitTime() { return submitTime; }
    public void setSubmitTime(Date submitTime) { this.submitTime = submitTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("batchId", getBatchId())
            .append("reportType", getReportType())
            .append("reportYear", getReportYear())
            .append("reportName", getReportName())
            .append("scopeDeptId", getScopeDeptId())
            .append("scopeDeptName", getScopeDeptName())
            .append("rowCount", getRowCount())
            .append("batchStatus", getBatchStatus())
            .append("genTime", getGenTime())
            .append("exportTime", getExportTime())
            .append("submitTime", getSubmitTime())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
