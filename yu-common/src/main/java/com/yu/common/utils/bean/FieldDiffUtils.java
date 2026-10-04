package com.yu.common.utils.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.yu.common.annotation.Excel;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.reflect.ReflectUtils;

/**
 * 字段级差异比对工具（K1 合规：字段级变更留痕的比对内核）。
 *
 * <p>给定「变更前对象」与「变更后对象」，逐字段比对并产出差异列表，供上层写入
 * {@code data_change_audit}。设计要点：</p>
 * <ul>
 *   <li><b>选择性更新语义</b>：新对象通常由表单绑定产生，未提交的字段为 {@code null}；
 *       对 {@code null} 的新值一律跳过（与 MyBatis {@code updateByPrimaryKeySelective}
 *       的「null 不更新」保持一致），避免把「未填」误记为「改成空」。</li>
 *   <li><b>噪声字段排除</b>：BaseEntity 的审计列与 transient/展示列不参与比对。</li>
 *   <li><b>中文标签自动发现</b>：字段标签优先取 {@link Excel#name()}，缺省回退字段名。</li>
 *   <li><b>敏感字段脱敏</b>：命中 {@code sensitiveFields} 的字段，其旧/新值均掩码后再落库，
 *       防止明文身份证/手机号进入审计流水。</li>
 * </ul>
 *
 * @author yu
 */
public class FieldDiffUtils
{
    /** 空值占位符（区分「字段本就为 null」与「空串」） */
    public static final String NULL_VALUE = "(空)";

    /**
     * 默认排除的噪声字段：BaseEntity 审计列 + 常见展示/技术字段。
     * 这些字段即使值不同也不计入业务变更留痕。
     */
    private static final Set<String> DEFAULT_EXCLUDE = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "serialVersionUID", "searchValue", "createBy", "createTime", "updateBy", "updateTime",
            "remark", "params", "dataScope", "pageDomain")));

    private FieldDiffUtils()
    {
    }

    /**
     * 比对新旧对象的全量业务字段差异（排除默认噪声字段，不做脱敏）。
     *
     * @param oldObj 变更前对象（一般为从库中重新查出的持久态）
     * @param newObj 变更后对象（表单绑定态，未提交字段为 null）
     * @return 差异列表，无差异时返回空列表
     */
    public static List<FieldChange> diff(Object oldObj, Object newObj)
    {
        return diff(oldObj, newObj, null, null);
    }

    /**
     * 比对指定跟踪字段的差异。
     *
     * @param oldObj        变更前对象
     * @param newObj        变更后对象
     * @param trackedFields 需要跟踪的字段名集合；为空表示比对全部非排除字段
     * @param sensitiveFields 敏感字段集合，命中则旧/新值掩码后落库；可为 null
     * @return 差异列表
     */
    public static List<FieldChange> diff(Object oldObj, Object newObj, Set<String> trackedFields, Set<String> sensitiveFields)
    {
        List<FieldChange> changes = new ArrayList<>();
        if (oldObj == null || newObj == null)
        {
            return changes;
        }
        Set<String> sensitive = sensitiveFields == null ? Collections.emptySet() : sensitiveFields;
        for (Field field : collectPersistentFields(newObj.getClass()))
        {
            String name = field.getName();
            if (DEFAULT_EXCLUDE.contains(name))
            {
                continue;
            }
            if (trackedFields != null && !trackedFields.isEmpty() && !trackedFields.contains(name))
            {
                continue;
            }
            Object oldVal = ReflectUtils.getFieldValue(oldObj, name);
            Object newVal = ReflectUtils.getFieldValue(newObj, name);
            // 选择性更新语义：新值为 null 视为「本次未提交该字段」，跳过
            if (newVal == null)
            {
                continue;
            }
            if (valueEquals(oldVal, newVal))
            {
                continue;
            }
            String label = resolveLabel(field, name);
            String oldStr = stringify(oldVal);
            String newStr = stringify(newVal);
            if (sensitive.contains(name))
            {
                oldStr = maskValue(oldStr);
                newStr = maskValue(newStr);
            }
            changes.add(new FieldChange(name, label, oldStr, newStr));
        }
        return changes;
    }

    /**
     * 收集类自身及其父类（不含 BaseEntity 以上）声明的实例字段，跳过 static/transient。
     */
    private static List<Field> collectPersistentFields(Class<?> clazz)
    {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass())
        {
            for (Field f : c.getDeclaredFields())
            {
                int mod = f.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || f.isSynthetic())
                {
                    continue;
                }
                fields.add(f);
            }
        }
        return fields;
    }

    private static String resolveLabel(Field field, String fallback)
    {
        Excel excel = field.getAnnotation(Excel.class);
        if (excel != null && StringUtils.isNotBlank(excel.name()))
        {
            return excel.name();
        }
        return fallback;
    }

    /**
     * 值等价判定：数值按 BigDecimal 语义比较（避免 85 与 85.0 误判为变更），其余用 equals。
     */
    private static boolean valueEquals(Object a, Object b)
    {
        if (a == null && b == null)
        {
            return true;
        }
        if (a == null || b == null)
        {
            return false;
        }
        if (a instanceof Number && b instanceof Number)
        {
            return new java.math.BigDecimal(a.toString())
                    .compareTo(new java.math.BigDecimal(b.toString())) == 0;
        }
        return a.equals(b);
    }

    /**
     * 敏感值掩码：保留首尾各 1 字符、中间以 * 遮蔽；长度 &le; 2 时全部遮蔽。
     * 用于身份证/手机号等敏感字段落审计流水前的脱敏。
     */
    private static String maskValue(String val)
    {
        if (StringUtils.isBlank(val))
        {
            return val;
        }
        int len = val.length();
        if (NULL_VALUE.equals(val))
        {
            return val;
        }
        if (len <= 2)
        {
            return StringUtils.repeat('*', len);
        }
        return val.charAt(0) + StringUtils.repeat('*', len - 2) + val.charAt(len - 1);
    }

    private static String stringify(Object val)
    {
        if (val == null)
        {
            return NULL_VALUE;
        }
        if (val instanceof Date)
        {
            return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, (Date) val);
        }
        return String.valueOf(val);
    }
}
