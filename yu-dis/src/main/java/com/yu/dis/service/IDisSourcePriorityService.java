package com.yu.dis.service;

import java.util.List;
import java.util.Map;
import com.yu.dis.arbitration.ArbitrationPreviewRequest;
import com.yu.dis.domain.DisSourcePriority;

/**
 * 主数据裁决数据源优先级 Service（F5）。
 */
public interface IDisSourcePriorityService
{
    public DisSourcePriority selectDisSourcePriorityByPriorityId(Long priorityId);

    public List<DisSourcePriority> selectDisSourcePriorityList(DisSourcePriority disSourcePriority);

    public int insertDisSourcePriority(DisSourcePriority disSourcePriority);

    public int updateDisSourcePriority(DisSourcePriority disSourcePriority);

    public int deleteDisSourcePriorityByPriorityIds(Long[] priorityIds);

    /** 将配置表启用项整体加载覆盖内存裁决策略基线（启动及每次增删改后调用）。 */
    public void reloadPolicy();

    /** 试裁决：给定业务主键与多源候选，按当前优先级配置裁决权威版本并解释依据。 */
    public Map<String, Object> preview(ArbitrationPreviewRequest request);
}
