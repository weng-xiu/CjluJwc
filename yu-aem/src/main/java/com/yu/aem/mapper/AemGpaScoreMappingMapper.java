package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemGpaScoreMapping;

/**
 * GPA分数段映射Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface AemGpaScoreMappingMapper 
{
    public List<AemGpaScoreMapping> selectByConfigId(Long configId);
    public int insertBatch(List<AemGpaScoreMapping> mappings);
    public int deleteByConfigId(Long configId);
}
