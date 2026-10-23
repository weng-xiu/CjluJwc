package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisSourcePriority;

/**
 * 主数据裁决数据源优先级 Mapper（F5）。
 */
public interface DisSourcePriorityMapper
{
    public DisSourcePriority selectDisSourcePriorityByPriorityId(Long priorityId);
    public List<DisSourcePriority> selectDisSourcePriorityList(DisSourcePriority disSourcePriority);
    /** 全部启用（status=0）配置，供裁决策略加载 */
    public List<DisSourcePriority> selectEnabledList();
    public int insertDisSourcePriority(DisSourcePriority disSourcePriority);
    public int updateDisSourcePriority(DisSourcePriority disSourcePriority);
    public int deleteDisSourcePriorityByPriorityId(Long priorityId);
    public int deleteDisSourcePriorityByPriorityIds(Long[] priorityIds);
}
