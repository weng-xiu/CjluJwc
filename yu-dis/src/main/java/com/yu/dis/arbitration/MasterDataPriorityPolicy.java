package com.yu.dis.arbitration;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 主数据源优先级集中策略（N2 主数据裁决）。
 *
 * <p>当同一业务主数据（如学籍、教师、课程）来自多个外部系统时，需要一个统一、可审计的
 * 优先级口径裁决"以谁为准"。此前 {@code dis_external_system} 无源优先级字段，多源并存时
 * 取数不确定。本策略把优先级收敛到单一登记处：数值越大越权威，未登记源回落到 {@link #DEFAULT_PRIORITY}。
 *
 * <p>F5 主数据裁决前端化后，优先级权威源迁移到 {@code dis_source_priority} 配置表，启动时由
 * {@code MasterDataPolicyInitializer} 经 {@link #replaceAll(Map)} 加载覆盖本基线；本构造函数保留为
 * 表未建立/无数据时的确定性回退基线（如离线单测环境），二者口径一致。
 *
 * @author N2
 */
@Component
public class MasterDataPriorityPolicy
{
    /** 未登记源的默认优先级 */
    public static final int DEFAULT_PRIORITY = 0;

    private final Map<String, Integer> priorities = new LinkedHashMap<>();

    public MasterDataPriorityPolicy()
    {
        // 学籍/成绩等核心主数据以研究生/教务主系统为权威源，示例基线可按实际调整
        register("GRAD", 100);   // 研究生学籍主系统
        register("JW", 90);      // 教务主系统
        register("HR", 80);      // 人事系统（教师主数据）
        register("FINANCE", 40); // 财务系统
        register("CARD", 20);    // 一卡通
    }

    /** 登记（或覆盖）某源编码的优先级 */
    public void register(String sourceCode, int priority)
    {
        priorities.put(sourceCode, priority);
    }

    /** 移除某源编码的优先级登记（配置删除/停用时联动） */
    public void unregister(String sourceCode)
    {
        priorities.remove(sourceCode);
    }

    /**
     * 以持久化配置整体替换内存基线（F5 前端化：配置表为权威源）。
     *
     * @param dbPriorities 源编码 -> 优先级；null 项忽略
     */
    public void replaceAll(Map<String, Integer> dbPriorities)
    {
        priorities.clear();
        if (dbPriorities != null)
        {
            dbPriorities.forEach((code, p) -> {
                if (code != null && p != null)
                {
                    priorities.put(code, p);
                }
            });
        }
    }

    /** 当前生效的优先级快照（源编码 -> 优先级），供裁决矩阵展示与试裁决解释 */
    public Map<String, Integer> snapshot()
    {
        return new LinkedHashMap<>(priorities);
    }

    /** 解析源优先级，未登记回落默认值 */
    public int resolvePriority(String sourceCode)
    {
        Integer p = sourceCode == null ? null : priorities.get(sourceCode);
        return p != null ? p : DEFAULT_PRIORITY;
    }
}
