package com.yu.common.utils.bean;

import java.io.Serializable;

/**
 * 字段级变更项。
 *
 * <p>由 {@link FieldDiffUtils} 在比对新旧对象时逐字段产出，供字段级变更流水
 * （data_change_audit）落库与展示。承载单个业务字段的「旧值 → 新值」差异，
 * 敏感字段的值在产出时即已脱敏（见 {@link FieldDiffUtils#diff}）。</p>
 *
 * @author yu
 */
public class FieldChange implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 字段名（Java 属性名，如 totalScore） */
    private String fieldName;

    /** 字段中文名（取自 @Excel(name=...)，缺省回退为字段名） */
    private String fieldLabel;

    /** 旧值（字符串化，null 记为空串占位 NULL_VALUE） */
    private String oldValue;

    /** 新值（字符串化） */
    private String newValue;

    public FieldChange()
    {
    }

    public FieldChange(String fieldName, String fieldLabel, String oldValue, String newValue)
    {
        this.fieldName = fieldName;
        this.fieldLabel = fieldLabel;
        this.oldValue = oldValue;
        this.newValue = newValue;
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
}
