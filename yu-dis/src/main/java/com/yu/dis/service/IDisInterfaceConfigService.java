package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisInterfaceConfig;

/**
 * 接口配置Service接口
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public interface IDisInterfaceConfigService 
{
    public DisInterfaceConfig selectDisInterfaceConfigByInterfaceId(Long interfaceId);
    public List<DisInterfaceConfig> selectDisInterfaceConfigList(DisInterfaceConfig disInterfaceConfig);
    public int insertDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig);
    public int updateDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig);
    public int deleteDisInterfaceConfigByInterfaceId(Long interfaceId);
    public int deleteDisInterfaceConfigByInterfaceIds(Long[] interfaceIds);
}
