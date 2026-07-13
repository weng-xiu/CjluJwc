package com.yu.aem.service.impl;

import java.util.List;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGpaAlgorithmConfigMapper;
import com.yu.aem.mapper.AemGpaScoreMappingMapper;
import com.yu.aem.domain.AemGpaAlgorithmConfig;
import com.yu.aem.domain.AemGpaScoreMapping;
import com.yu.aem.service.IGpaAlgorithmConfigService;
import com.yu.aem.strategy.GpaStrategyFactory;

/**
 * GPA算法配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class GpaAlgorithmConfigServiceImpl implements IGpaAlgorithmConfigService
{
    @Autowired
    private AemGpaAlgorithmConfigMapper aemGpaAlgorithmConfigMapper;

    @Autowired
    private AemGpaScoreMappingMapper aemGpaScoreMappingMapper;

    @Autowired
    private GpaStrategyFactory gpaStrategyFactory;

    @Override
    public List<AemGpaAlgorithmConfig> selectAlgorithmConfigList(AemGpaAlgorithmConfig config)
    {
        return aemGpaAlgorithmConfigMapper.selectAemGpaAlgorithmConfigList(config);
    }

    @Override
    public AemGpaAlgorithmConfig selectAlgorithmConfigById(Long configId)
    {
        return aemGpaAlgorithmConfigMapper.selectAemGpaAlgorithmConfigById(configId);
    }

    @Override
    @Transactional
    public int insertAlgorithmConfig(AemGpaAlgorithmConfig config)
    {
        config.setCreateTime(DateUtils.getNowDate());
        if (StringUtils.isEmpty(config.getIsDefault()))
        {
            config.setIsDefault("0");
        }
        if (StringUtils.isEmpty(config.getStatus()))
        {
            config.setStatus("0");
        }
        int result = aemGpaAlgorithmConfigMapper.insertAemGpaAlgorithmConfig(config);
        refreshCustomStrategy(config);
        return result;
    }

    @Override
    @Transactional
    public int updateAlgorithmConfig(AemGpaAlgorithmConfig config)
    {
        config.setUpdateTime(DateUtils.getNowDate());
        int result = aemGpaAlgorithmConfigMapper.updateAemGpaAlgorithmConfig(config);
        refreshCustomStrategy(config);
        return result;
    }

    @Override
    @Transactional
    public int deleteAlgorithmConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            aemGpaScoreMappingMapper.deleteByConfigId(configId);
        }
        return aemGpaAlgorithmConfigMapper.deleteAemGpaAlgorithmConfigByIds(configIds);
    }

    @Override
    @Transactional
    public int setDefaultAlgorithm(Long configId)
    {
        aemGpaAlgorithmConfigMapper.updateAllDefaultToZero();
        AemGpaAlgorithmConfig config = new AemGpaAlgorithmConfig();
        config.setConfigId(configId);
        config.setIsDefault("1");
        config.setUpdateTime(DateUtils.getNowDate());
        return aemGpaAlgorithmConfigMapper.updateAemGpaAlgorithmConfig(config);
    }

    @Override
    public String getDefaultAlgorithmCode()
    {
        AemGpaAlgorithmConfig config = aemGpaAlgorithmConfigMapper.selectDefaultAlgorithm();
        if (config != null && StringUtils.isNotEmpty(config.getAlgorithmCode()))
        {
            return config.getAlgorithmCode();
        }
        return "CN_STANDARD";
    }

    @Override
    public List<AemGpaScoreMapping> selectScoreMappings(Long configId)
    {
        return aemGpaScoreMappingMapper.selectByConfigId(configId);
    }

    @Override
    @Transactional
    public int saveScoreMappings(Long configId, List<AemGpaScoreMapping> mappings)
    {
        aemGpaScoreMappingMapper.deleteByConfigId(configId);
        int result = 0;
        if (mappings != null && !mappings.isEmpty())
        {
            for (int i = 0; i < mappings.size(); i++)
            {
                AemGpaScoreMapping mapping = mappings.get(i);
                mapping.setConfigId(configId);
                if (mapping.getSortOrder() == null)
                {
                    mapping.setSortOrder(i + 1);
                }
            }
            result = aemGpaScoreMappingMapper.insertBatch(mappings);
        }
        refreshCustomStrategy(selectAlgorithmConfigById(configId));
        return result;
    }

    /**
     * 刷新自定义映射策略缓存（仅针对非内置算法）
     */
    private void refreshCustomStrategy(AemGpaAlgorithmConfig config)
    {
        if (config == null || config.getAlgorithmCode() == null)
        {
            return;
        }
        String code = config.getAlgorithmCode();
        if ("CN_STANDARD".equals(code) || "GPA_4_0".equals(code))
        {
            return;
        }
        List<AemGpaScoreMapping> mappings = aemGpaScoreMappingMapper.selectByConfigId(config.getConfigId());
        gpaStrategyFactory.registerCustomStrategy(code, mappings);
    }
}
