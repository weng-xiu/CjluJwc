package com.yu.dis.mapper;

import java.util.List;
import com.yu.dis.domain.DisFieldMapping;

/**
 * 数据同步字段映射Mapper接口
 *
 * @author ruoyi
 */
public interface DisFieldMappingMapper
{
    /** 查询某接口下启用（status=0）的字段映射，按排序返回 */
    public List<DisFieldMapping> selectEnabledByInterfaceId(Long interfaceId);

    public DisFieldMapping selectDisFieldMappingByMappingId(Long mappingId);

    public List<DisFieldMapping> selectDisFieldMappingList(DisFieldMapping disFieldMapping);

    public int insertDisFieldMapping(DisFieldMapping disFieldMapping);

    public int updateDisFieldMapping(DisFieldMapping disFieldMapping);

    public int deleteDisFieldMappingByMappingId(Long mappingId);

    public int deleteDisFieldMappingByMappingIds(Long[] mappingIds);
}
