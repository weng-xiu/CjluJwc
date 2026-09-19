package com.yu.dis.sync;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yu.dis.domain.DisFieldMapping;

/**
 * 同步响应解析器（D1 链路"解析"环节）。
 *
 * 职责：将外部接口返回的 JSON 响应体，依据 {@link DisFieldMapping} 字段映射配置，
 * 解析为"目标业务表 -> 行数据列表"的结构，供落库环节使用。
 *
 * 该组件为无状态纯逻辑，不依赖 Spring / 网络 / 数据库，便于离线单元测试。
 *
 * @author ruoyi
 */
public class SyncResponseParser
{
    /** 合法 SQL 标识符（表名/列名）：字母或下划线开头，后接字母数字下划线 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** 常见承载数据数组的顶层键（按优先级探测） */
    private static final String[] DATA_KEYS = {"data", "rows", "result", "list", "items", "records", "content"};

    /**
     * 解析响应体。
     *
     * @param responseBody 外部接口原始响应（JSON 字符串）
     * @param mappings     字段映射配置（同一接口的全部启用映射）
     * @return 目标表 -> 行数据（列名 -> 值）的有序映射；无数据时返回空 Map
     */
    public Map<String, List<Map<String, Object>>> parse(String responseBody, List<DisFieldMapping> mappings)
    {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (responseBody == null || responseBody.trim().isEmpty() || mappings == null || mappings.isEmpty())
        {
            return result;
        }
        Object root;
        try
        {
            root = JSON.parse(responseBody);
        }
        catch (Exception e)
        {
            // 非 JSON 响应，视为无可解析数据
            return result;
        }
        JSONArray records = locateRecordArray(root);
        if (records == null || records.isEmpty())
        {
            return result;
        }
        // 按目标表分组映射；同时校验标识符合法性，非法则跳过该映射（防注入）
        Map<String, List<DisFieldMapping>> byTable = new LinkedHashMap<>();
        for (DisFieldMapping m : mappings)
        {
            if (!isSafeIdentifier(m.getTargetTable()) || !isSafeIdentifier(m.getTargetColumn()))
            {
                continue;
            }
            if (m.getSourceField() == null || m.getSourceField().trim().isEmpty())
            {
                continue;
            }
            byTable.computeIfAbsent(m.getTargetTable(), k -> new ArrayList<>()).add(m);
        }
        for (Map.Entry<String, List<DisFieldMapping>> entry : byTable.entrySet())
        {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int i = 0; i < records.size(); i++)
            {
                Object element = records.get(i);
                if (!(element instanceof JSONObject))
                {
                    continue;
                }
                JSONObject obj = (JSONObject) element;
                Map<String, Object> row = new LinkedHashMap<>();
                boolean hasValue = false;
                for (DisFieldMapping m : entry.getValue())
                {
                    Object value = extractByPath(obj, m.getSourceField());
                    if (value != null)
                    {
                        row.put(m.getTargetColumn(), value);
                        hasValue = true;
                    }
                }
                if (hasValue)
                {
                    rows.add(row);
                }
            }
            if (!rows.isEmpty())
            {
                result.put(entry.getKey(), rows);
            }
        }
        return result;
    }

    /**
     * 定位响应中的记录数组：
     * 1) 顶层即为数组；
     * 2) 顶层对象下常见 data/rows/... 键对应的数组；
     * 3) 顶层对象下第一个出现的数组值（兜底）。
     */
    private JSONArray locateRecordArray(Object root)
    {
        if (root instanceof JSONArray)
        {
            return (JSONArray) root;
        }
        if (root instanceof JSONObject)
        {
            JSONObject obj = (JSONObject) root;
            for (String key : DATA_KEYS)
            {
                Object v = obj.get(key);
                if (v instanceof JSONArray && !((JSONArray) v).isEmpty())
                {
                    return (JSONArray) v;
                }
            }
            // 数据可能嵌套一层，如 data.list / data.rows
            for (String key : DATA_KEYS)
            {
                Object v = obj.get(key);
                if (v instanceof JSONObject)
                {
                    JSONArray nested = locateRecordArray(v);
                    if (nested != null)
                    {
                        return nested;
                    }
                }
            }
            for (String k : obj.keySet())
            {
                Object v = obj.get(k);
                if (v instanceof JSONArray && !((JSONArray) v).isEmpty())
                {
                    return (JSONArray) v;
                }
            }
        }
        return null;
    }

    /**
     * 按点分路径从对象中提取值，支持 a.b.c 嵌套；不支持路径语法时按原始键读取。
     */
    private Object extractByPath(JSONObject obj, String path)
    {
        if (obj.containsKey(path))
        {
            return obj.get(path);
        }
        if (path.indexOf('.') < 0)
        {
            return null;
        }
        String[] segments = path.split("\\.");
        Object current = obj;
        for (String seg : segments)
        {
            if (current instanceof JSONObject)
            {
                current = ((JSONObject) current).get(seg);
                if (current == null)
                {
                    return null;
                }
            }
            else
            {
                return null;
            }
        }
        return current;
    }

    /** 校验表名/列名是否为安全 SQL 标识符 */
    public static boolean isSafeIdentifier(String identifier)
    {
        return identifier != null && SAFE_IDENTIFIER.matcher(identifier).matches();
    }
}
