package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.core.text.Convert;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.bean.FieldChange;
import com.yu.common.utils.bean.FieldDiffUtils;
import com.yu.system.domain.SysConfig;
import com.yu.system.mapper.SysConfigMapper;
import com.yu.system.service.IDataChangeAuditService;
import com.yu.system.service.ISysConfigService;

/**
 * 参数配置 服务层实现
 * 
 * @author ruoyi
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService
{
    @Autowired
    private SysConfigMapper configMapper;

    @Autowired
    private RedisCache redisCache;

    /** A4/K1 配置项级审计：参数变更须「可追溯、可核查」，复用字段级流水基建 sys_config 接入 data_change_audit */
    @Autowired
    private IDataChangeAuditService dataChangeAuditService;

    /** 参数变更留痕跟踪字段：仅比对这些业务列（remark/createBy 等审计列已被 FieldDiffUtils 默认排除） */
    private static final Set<String> CONFIG_TRACKED_FIELDS = new HashSet<>(Arrays.asList(
            "configName", "configKey", "configValue", "configType"));

    /** 参数变更流水的实体类型标识（与表名一致，供审计查询归类） */
    private static final String CONFIG_ENTITY_TYPE = "sys_config";

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     * 
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfig selectConfigById(Long configId)
    {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        return configMapper.selectConfig(config);
    }

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        String configValue = Convert.toStr(redisCache.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue))
        {
            return configValue;
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(configKey);
        SysConfig retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig))
        {
            redisCache.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue(), CacheConstants.SYS_CONFIG_CACHE_EXPIRATION, TimeUnit.MINUTES);
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取验证码开关
     * 
     * @return true开启，false关闭
     */
    @Override
    public boolean selectCaptchaEnabled()
    {
        String captchaEnabled = selectConfigByKey("sys.account.captchaEnabled");
        if (StringUtils.isEmpty(captchaEnabled))
        {
            return true;
        }
        return Convert.toBool(captchaEnabled);
    }

    /**
     * 查询参数配置列表
     * 
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config)
    {
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertConfig(SysConfig config)
    {
        int row = configMapper.insertConfig(config);
        if (row > 0)
        {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue(), CacheConstants.SYS_CONFIG_CACHE_EXPIRATION, TimeUnit.MINUTES);
            // A4/K1 配置项级审计：记录参数新建（空 → 初始键值），保证参数全生命周期可追溯
            List<FieldChange> changes = new ArrayList<>();
            changes.add(new FieldChange("configValue", "参数键值", FieldDiffUtils.NULL_VALUE, config.getConfigValue()));
            dataChangeAuditService.recordDiff(CONFIG_ENTITY_TYPE, "参数配置", String.valueOf(config.getConfigId()), changes);
        }
        return row;
    }

    /**
     * 修改参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateConfig(SysConfig config)
    {
        SysConfig temp = configMapper.selectConfigById(config.getConfigId());
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey()))
        {
            redisCache.deleteObject(getCacheKey(temp.getConfigKey()));
        }

        int row = configMapper.updateConfig(config);
        if (row > 0)
        {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue(), CacheConstants.SYS_CONFIG_CACHE_EXPIRATION, TimeUnit.MINUTES);
            // A4/K1 配置项级审计：以「库中旧值 vs 提交新值」比对产出字段级流水（参数非个人 PII，明文留存供核查）
            if (temp != null)
            {
                List<FieldChange> changes = FieldDiffUtils.diff(temp, config, CONFIG_TRACKED_FIELDS, Collections.emptySet());
                dataChangeAuditService.recordDiff(CONFIG_ENTITY_TYPE, "参数配置", String.valueOf(config.getConfigId()), changes);
            }
        }
        return row;
    }

    /**
     * 批量删除参数信息
     * 
     * @param configIds 需要删除的参数ID
     */
    @Override
    @Transactional
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configMapper.deleteConfigById(configId);
            redisCache.deleteObject(getCacheKey(config.getConfigKey()));
            // A4/K1 配置项级审计：记录参数删除（原键值 → 空）
            List<FieldChange> changes = new ArrayList<>();
            changes.add(new FieldChange("configValue", "参数键值", config.getConfigValue(), FieldDiffUtils.NULL_VALUE));
            dataChangeAuditService.recordDiff(CONFIG_ENTITY_TYPE, "参数配置", String.valueOf(configId), changes);
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<SysConfig> configsList = configMapper.selectConfigList(new SysConfig());
        for (SysConfig config : configsList)
        {
            redisCache.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue(), CacheConstants.SYS_CONFIG_CACHE_EXPIRATION, TimeUnit.MINUTES);
        }
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache()
    {
        Collection<String> keys = redisCache.keys(CacheConstants.SYS_CONFIG_KEY + "*");
        redisCache.deleteObject(keys);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
