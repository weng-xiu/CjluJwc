package com.yu.system.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.annotation.Excel.ColumnType;
import com.yu.common.core.domain.BaseEntity;

/**
 * 字段级数据变更流水对象 data_change_audit（K1 合规③）。
 *
 * <p>记录关键业务实体（成绩/学籍等）单字段级别的「旧值 → 新值」变更，含操作人、
 * 来源 IP、链路 traceId 与变更时间，满足等保「数据操作可追溯、可核查」要求。
 * 与 {@link SysOperLog}（操作级日志）互补：本表下沉到字段粒度。</p>
 *
 * @author yu
 */
public class DataChangeAudit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Excel(name = "流水号", cellType = ColumnType.NUMERIC)
    private Long auditId;

    /** 实体类型（表名或类简名，如 aem_grade_record） */
    @Excel(name = "实体类型")
    private String entityType;

    /** 实体类型中文名（如 成绩记录） */
    @Excel(name = "实体名称")
    private String entityTypeLabel;

    /** 业务主键值 */
    @Excel(name = "业务主键")
    private String bizId;

    /** 变更字段名 */
    @Excel(name = "字段名")
    private String fieldName;

    /** 变更字段中文名 */
    @Excel(name = "字段名称")
    private String fieldLabel;

    /** 旧值（敏感字段已脱敏） */
    @Excel(name = "旧值")
    private String oldValue;

    /** 新值（敏感字段已脱敏） */
    @Excel(name = "新值")
    private String newValue;

    /** 操作人登录名 */
    @Excel(name = "操作人")
    private String operName;

    /** 操作人 IP */
    @Excel(name = "操作IP")
    private String operIp;

    /** 链路追踪 ID */
    private String traceId;

    /** 变更时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "变更时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    public Long getAuditId()
    {
        return auditId;
    }

    public void setAuditId(Long auditId)
    {
        this.auditId = auditId;
    }

    public String getEntityType()
    {
        return entityType;
    }

    public void setEntityType(String entityType)
    {
        this.entityType = entityType;
    }

    public String getEntityTypeLabel()
    {
        return entityTypeLabel;
    }

    public void setEntityTypeLabel(String entityTypeLabel)
    {
        this.entityTypeLabel = entityTypeLabel;
    }

    public String getBizId()
    {
        return bizId;
    }

    public void setBizId(String bizId)
    {
        this.bizId = bizId;
    }

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
    }

    public String getFieldLabel()
    {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel)
    {
        this.fieldLabel = fieldLabel;
    }

    public String getOldValue()
    {
        return oldValue;
    }

    public void setOldValue(String oldValue)
    {
        this.oldValue = oldValue;
    }

    public String getNewValue()
    {
        return newValue;
    }

    public void setNewValue(String newValue)
    {
        this.newValue = newValue;
    }

    public String getOperName()
    {
        return operName;
    }

    public void setOperName(String operName)
    {
        this.operName = operName;
    }

    public String getOperIp()
    {
        return operIp;
    }

    public void setOperIp(String operIp)
    {
        this.operIp = operIp;
    }

    public String getTraceId()
    {
        return traceId;
    }

    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }

    public Date getChangeTime()
    {
        return changeTime;
    }

    public void setChangeTime(Date changeTime)
    {
        this.changeTime = changeTime;
    }
}
