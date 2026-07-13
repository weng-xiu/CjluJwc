package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamWarningRuleConfigMapper;
import com.yu.sam.domain.SamWarningRuleConfig;
import com.yu.sam.service.ISamWarningRuleConfigService;

/**
 * 预警规则配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class SamWarningRuleConfigServiceImpl implements ISamWarningRuleConfigService 
{
    @Autowired
    private SamWarningRuleConfigMapper samWarningRuleConfigMapper;

    @Override
    public SamWarningRuleConfig selectSamWarningRuleConfigById(Long ruleId) 
    {
        return samWarningRuleConfigMapper.selectSamWarningRuleConfigById(ruleId);
    }

    @Override
    public List<SamWarningRuleConfig> selectSamWarningRuleConfigList(SamWarningRuleConfig samWarningRuleConfig) 
    {
        return samWarningRuleConfigMapper.selectSamWarningRuleConfigList(samWarningRuleConfig);
    }

    @Override
    public List<SamWarningRuleConfig> selectEnabledRules() 
    {
        return samWarningRuleConfigMapper.selectEnabledRules();
    }

    @Override
    public SamWarningRuleConfig selectByRuleCode(String ruleCode) 
    {
        return samWarningRuleConfigMapper.selectByRuleCode(ruleCode);
    }

    @Override
    @Transactional
    public int insertSamWarningRuleConfig(SamWarningRuleConfig samWarningRuleConfig) 
    {
        samWarningRuleConfig.setCreateTime(DateUtils.getNowDate());
        return samWarningRuleConfigMapper.insertSamWarningRuleConfig(samWarningRuleConfig);
    }

    @Override
    @Transactional
    public int updateSamWarningRuleConfig(SamWarningRuleConfig samWarningRuleConfig) 
    {
        samWarningRuleConfig.setUpdateTime(DateUtils.getNowDate());
        return samWarningRuleConfigMapper.updateSamWarningRuleConfig(samWarningRuleConfig);
    }

    @Override
    @Transactional
    public int deleteSamWarningRuleConfigById(Long ruleId) 
    {
        return samWarningRuleConfigMapper.deleteSamWarningRuleConfigById(ruleId);
    }

    @Override
    @Transactional
    public int deleteSamWarningRuleConfigByIds(Long[] ruleIds) 
    {
        return samWarningRuleConfigMapper.deleteSamWarningRuleConfigByIds(ruleIds);
    }
}
