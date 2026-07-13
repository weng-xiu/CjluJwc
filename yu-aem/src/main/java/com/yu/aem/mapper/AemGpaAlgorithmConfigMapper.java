package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemGpaAlgorithmConfig;

/**
 * GPA算法配置Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface AemGpaAlgorithmConfigMapper 
{
    public AemGpaAlgorithmConfig selectAemGpaAlgorithmConfigById(Long configId);
    public List<AemGpaAlgorithmConfig> selectAemGpaAlgorithmConfigList(AemGpaAlgorithmConfig aemGpaAlgorithmConfig);
    public int insertAemGpaAlgorithmConfig(AemGpaAlgorithmConfig aemGpaAlgorithmConfig);
    public int updateAemGpaAlgorithmConfig(AemGpaAlgorithmConfig aemGpaAlgorithmConfig);
    public int deleteAemGpaAlgorithmConfigById(Long configId);
    public int deleteAemGpaAlgorithmConfigByIds(Long[] configIds);
    public AemGpaAlgorithmConfig selectDefaultAlgorithm();
    public int updateAllDefaultToZero();
}
