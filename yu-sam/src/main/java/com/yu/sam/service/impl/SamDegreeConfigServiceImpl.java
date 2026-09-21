package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamDegreeConfigMapper;
import com.yu.sam.domain.SamDegreeConfig;
import com.yu.sam.service.ISamDegreeConfigService;

/**
 * 学位授予条件配置Service业务层处理
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Service
public class SamDegreeConfigServiceImpl implements ISamDegreeConfigService
{
    /** 内置默认 GPA 阈值（无配置时回退，保持历史口径） */
    private static final double DEFAULT_GPA_THRESHOLD = 2.0;

    @Autowired
    private SamDegreeConfigMapper samDegreeConfigMapper;

    @Override
    public SamDegreeConfig selectSamDegreeConfigByConfigId(Long configId)
    {
        return samDegreeConfigMapper.selectSamDegreeConfigByConfigId(configId);
    }

    @Override
    public List<SamDegreeConfig> selectSamDegreeConfigList(SamDegreeConfig samDegreeConfig)
    {
        return samDegreeConfigMapper.selectSamDegreeConfigList(samDegreeConfig);
    }

    @Override
    @Transactional
    public int insertSamDegreeConfig(SamDegreeConfig samDegreeConfig)
    {
        samDegreeConfig.setCreateTime(DateUtils.getNowDate());
        // 首条配置自动置为默认；设为默认时清除其它默认标记
        boolean first = samDegreeConfigMapper.selectSamDegreeConfigList(new SamDegreeConfig()).isEmpty();
        if (first)
        {
            samDegreeConfig.setIsDefault("1");
        }
        if ("1".equals(samDegreeConfig.getIsDefault()))
        {
            samDegreeConfigMapper.clearDefaultFlag();
        }
        return samDegreeConfigMapper.insertSamDegreeConfig(samDegreeConfig);
    }

    @Override
    @Transactional
    public int updateSamDegreeConfig(SamDegreeConfig samDegreeConfig)
    {
        if ("1".equals(samDegreeConfig.getIsDefault()))
        {
            samDegreeConfigMapper.clearDefaultFlag();
        }
        samDegreeConfig.setUpdateTime(DateUtils.getNowDate());
        return samDegreeConfigMapper.updateSamDegreeConfig(samDegreeConfig);
    }

    @Override
    public int deleteSamDegreeConfigByConfigId(Long configId)
    {
        SamDegreeConfig cfg = samDegreeConfigMapper.selectSamDegreeConfigByConfigId(configId);
        if (cfg != null && "1".equals(cfg.getIsDefault()))
        {
            throw new ServiceException("默认配置不可删除，请先指定其它默认配置");
        }
        return samDegreeConfigMapper.deleteSamDegreeConfigByConfigId(configId);
    }

    @Override
    @Transactional
    public int deleteSamDegreeConfigByConfigIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SamDegreeConfig cfg = samDegreeConfigMapper.selectSamDegreeConfigByConfigId(configId);
            if (cfg != null && "1".equals(cfg.getIsDefault()))
            {
                throw new ServiceException("默认配置不可删除，请先指定其它默认配置");
            }
        }
        return samDegreeConfigMapper.deleteSamDegreeConfigByConfigIds(configIds);
    }

    @Override
    @Transactional
    public int setDefault(Long configId)
    {
        SamDegreeConfig cfg = samDegreeConfigMapper.selectSamDegreeConfigByConfigId(configId);
        if (cfg == null)
        {
            throw new ServiceException("配置不存在");
        }
        samDegreeConfigMapper.clearDefaultFlag();
        SamDegreeConfig update = new SamDegreeConfig();
        update.setConfigId(configId);
        update.setIsDefault("1");
        update.setUpdateTime(DateUtils.getNowDate());
        return samDegreeConfigMapper.updateSamDegreeConfig(update);
    }

    @Override
    public SamDegreeConfig resolveEffective()
    {
        SamDegreeConfig cfg = samDegreeConfigMapper.selectDefaultConfig();
        if (cfg == null)
        {
            // 无配置时回退内置默认，保持与旧硬编码一致的历史口径
            cfg = new SamDegreeConfig();
            cfg.setGpaThreshold(DEFAULT_GPA_THRESHOLD);
            cfg.setRequireDegreeCourse("1");
            cfg.setRequireForeignLanguage("0");
            cfg.setRequireThesis("0");
            cfg.setRequireAchievement("0");
        }
        // 字段级兜底（防止历史数据 NULL）
        if (cfg.getGpaThreshold() == null)
        {
            cfg.setGpaThreshold(DEFAULT_GPA_THRESHOLD);
        }
        if (cfg.getRequireDegreeCourse() == null)
        {
            cfg.setRequireDegreeCourse("1");
        }
        if (cfg.getRequireForeignLanguage() == null)
        {
            cfg.setRequireForeignLanguage("0");
        }
        if (cfg.getRequireThesis() == null)
        {
            cfg.setRequireThesis("0");
        }
        if (cfg.getRequireAchievement() == null)
        {
            cfg.setRequireAchievement("0");
        }
        return cfg;
    }
}
