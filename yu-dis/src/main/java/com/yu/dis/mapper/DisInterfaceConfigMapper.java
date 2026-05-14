package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisInterfaceConfig;

public interface DisInterfaceConfigMapper 
{
    public DisInterfaceConfig selectDisInterfaceConfigByInterfaceId(Long interfaceId);
    public List<DisInterfaceConfig> selectDisInterfaceConfigList(DisInterfaceConfig disInterfaceConfig);
    public int insertDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig);
    public int updateDisInterfaceConfig(DisInterfaceConfig disInterfaceConfig);
    public int deleteDisInterfaceConfigByInterfaceId(Long interfaceId);
    public int deleteDisInterfaceConfigByInterfaceIds(Long[] interfaceIds);
}
