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
 * <p>后续可将优先级持久化到外部系统表并在此加载，当前以集中配置形式提供确定的裁决基线。
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

    /** 解析源优先级，未登记回落默认值 */
    public int resolvePriority(String sourceCode)
    {
        Integer p = sourceCode == null ? null : priorities.get(sourceCode);
        return p != null ? p : DEFAULT_PRIORITY;
    }
}
