package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGpaAlgorithmConfig;
import com.yu.aem.domain.AemGpaScoreMapping;

/**
 * GPA算法配置Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IGpaAlgorithmConfigService
{
    public List<AemGpaAlgorithmConfig> selectAlgorithmConfigList(AemGpaAlgorithmConfig config);
    public AemGpaAlgorithmConfig selectAlgorithmConfigById(Long configId);
    public int insertAlgorithmConfig(AemGpaAlgorithmConfig config);
    public int updateAlgorithmConfig(AemGpaAlgorithmConfig config);
    public int deleteAlgorithmConfigByIds(Long[] configIds);
    public int setDefaultAlgorithm(Long configId);
    public String getDefaultAlgorithmCode();

    /** 查询指定算法的分数段映射 */
    public List<AemGpaScoreMapping> selectScoreMappings(Long configId);

    /** 保存分数段映射（先删后插） */
    public int saveScoreMappings(Long configId, List<AemGpaScoreMapping> mappings);
}
