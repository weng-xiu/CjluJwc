package com.yu.dis.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.dis.mapper.DisInterfaceConfigMapper;
import com.yu.dis.domain.DisInterfaceConfig;
import com.yu.dis.service.IDisInterfaceConfigService;

/**
 * 接口配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
@Service
public class DisInterfaceConfigServiceImpl implements IDisInterfaceConfigService 
{
    @Autowired
    private DisInterfaceConfigMapper disInterfaceConfigMapper;

    @Override
    public DisInterfaceConfig selectDisInterfaceConfigByInterfaceId(Long interfaceId)
    {
        return disInterfaceConfigMapper.selectDisInterfaceConfigByInterfaceId(interfaceId);
    }

    @Override
    public List<DisInterfaceConfig> selectDisInterfaceConfigList(DisInterfaceConfig disInterfaceConfig)
    {
        return disInterfaceConfigMapper.selectDisInterfaceConfigList(disInterfaceConfig);
    }

    @Transactional
    @Override
    public int insertDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig)
    {
        disInterfaceConfig.setCreateTime(DateUtils.getNowDate());
        return disInterfaceConfigMapper.insertDisInterfaceConfig(disInterfaceConfig);
    }

    @Transactional
    @Override
    public int updateDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig)
    {
        disInterfaceConfig.setUpdateTime(DateUtils.getNowDate());
        return disInterfaceConfigMapper.updateDisInterfaceConfig(disInterfaceConfig);
    }

    @Transactional
    @Override
    public int deleteDisInterfaceConfigByInterfaceId(Long interfaceId)
    {
        return disInterfaceConfigMapper.deleteDisInterfaceConfigByInterfaceId(interfaceId);
    }

    @Transactional
    @Override
    public int deleteDisInterfaceConfigByInterfaceIds(Long[] interfaceIds)
    {
        return disInterfaceConfigMapper.deleteDisInterfaceConfigByInterfaceIds(interfaceIds);
    }
}
