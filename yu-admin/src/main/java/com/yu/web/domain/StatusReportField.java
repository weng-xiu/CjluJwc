package com.yu.web.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;

/**
 * 状态数据上报字段映射对象 sys_status_report_field
 *
 * <p>登记每类上报数据的标准字段代码、名称、来源与值域转换规则，
 * 既作为页面口径说明，也驱动预览列生成，保证上报映射可维护、可追溯。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusReportField extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 字段ID */
    private Long fieldId;

    /** 上报类型（01学生 02课程 03成绩 04教师） */
    private String reportType;

    /** 标准字段代码 */
    private String stdCode;

    /** 标准字段名称 */
    private String stdName;

    /** 数据来源（表.字段） */
    private String sourceExpr;

    /** 值域/转换规则 */
    private String convertRule;

    /** 数据类型（S字符 N数值 D日期） */
    private String dataType;

    /** 是否必填（0否 1是） */
    private String required;

    /** 展示顺序 */
    private Integer orderNum;

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getStdCode() { return stdCode; }
    public void setStdCode(String stdCode) { this.stdCode = stdCode; }
    public String getStdName() { return stdName; }
    public void setStdName(String stdName) { this.stdName = stdName; }
    public String getSourceExpr() { return sourceExpr; }
    public void setSourceExpr(String sourceExpr) { this.sourceExpr = sourceExpr; }
    public String getConvertRule() { return convertRule; }
    public void setConvertRule(String convertRule) { this.convertRule = convertRule; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }
    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("fieldId", getFieldId())
            .append("reportType", getReportType())
            .append("stdCode", getStdCode())
            .append("stdName", getStdName())
            .append("sourceExpr", getSourceExpr())
            .append("convertRule", getConvertRule())
            .append("dataType", getDataType())
            .append("required", getRequired())
            .append("orderNum", getOrderNum())
            .toString();
    }
}
