package com.yu.dis.arbitration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 主数据裁决器（N2 主数据裁决）。
 *
 * <p>对同一业务主键（businessKey）来自多个外部系统的候选记录进行裁决，输出唯一的权威版本：
 * <ol>
 *   <li>先按源优先级（{@link MasterDataPriorityPolicy}）降序；</li>
 *   <li>优先级相同再按更新时间 {@code updateTime} 最新优先；</li>
 *   <li>仍相同则保持先出现的稳定顺序。</li>
 * </ol>
 *
 * <p>裁决逻辑为纯内存规则，可独立单测，且完全确定，便于审计证明"以谁为准"。
 *
 * @author N2
 */
@Component
public class MasterDataArbiter
{
    @Autowired
    private MasterDataPriorityPolicy priorityPolicy;

    /**
     * 一条候选主数据。
     *
     * @param businessKey 业务主键（同一逻辑实体共享，如学号/工号）
     * @param sourceCode  来源系统编码
     * @param updateTime  该来源记录的更新时间
     * @param value       主数据载荷
     */
    public record MasterCandidate(String businessKey, String sourceCode, Date updateTime, Object value)
    {
    }

    /**
     * 从同一业务主键的多个候选中裁决出权威记录。
     *
     * @return 胜出候选；候选为空返回 {@code null}
     */
    public MasterCandidate pickWinner(List<MasterCandidate> candidates)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return null;
        }
        return candidates.stream()
                .max(Comparator
                        .comparingInt((MasterCandidate c) -> priorityPolicy.resolvePriority(c.sourceCode()))
                        .thenComparingLong(c -> c.updateTime() == null ? 0L : c.updateTime().getTime()))
                .orElse(null);
    }

    /**
     * 批量裁决：按业务主键分组，每组输出权威记录。
     *
     * @param candidates 全部候选
     * @return businessKey -> 权威记录（保持入参出现顺序）
     */
    public Map<String, MasterCandidate> arbitrate(List<MasterCandidate> candidates)
    {
        Map<String, List<MasterCandidate>> grouped = new LinkedHashMap<>();
        if (candidates != null)
        {
            for (MasterCandidate c : candidates)
            {
                grouped.computeIfAbsent(c.businessKey(), k -> new ArrayList<>()).add(c);
            }
        }
        Map<String, MasterCandidate> result = new LinkedHashMap<>();
        grouped.forEach((key, list) -> result.put(key, pickWinner(list)));
        return result;
    }
}
