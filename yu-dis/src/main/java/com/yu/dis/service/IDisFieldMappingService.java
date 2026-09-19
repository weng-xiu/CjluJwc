package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisFieldMapping;

/**
 * 数据同步字段映射Service接口
 *
 * @author ruoyi
 */
public interface IDisFieldMappingService
{
    public DisFieldMapping selectDisFieldMappingByMappingId(Long mappingId);

    public List<DisFieldMapping> selectDisFieldMappingList(DisFieldMapping disFieldMapping);

    public List<DisFieldMapping> selectEnabledByInterfaceId(Long interfaceId);

    public int insertDisFieldMapping(DisFieldMapping disFieldMapping);

    public int updateDisFieldMapping(DisFieldMapping disFieldMapping);

    public int deleteDisFieldMappingByMappingId(Long mappingId);

    public int deleteDisFieldMappingByMappingIds(Long[] mappingIds);
}
