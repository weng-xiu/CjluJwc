package com.yu.dis.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.dis.mapper.DisFieldMappingMapper;
import com.yu.dis.domain.DisFieldMapping;
import com.yu.dis.service.IDisFieldMappingService;

/**
 * 数据同步字段映射Service业务层处理
 *
 * @author ruoyi
 */
@Service
public class DisFieldMappingServiceImpl implements IDisFieldMappingService
{
    @Autowired
    private DisFieldMappingMapper disFieldMappingMapper;

    @Override
    public DisFieldMapping selectDisFieldMappingByMappingId(Long mappingId)
    {
        return disFieldMappingMapper.selectDisFieldMappingByMappingId(mappingId);
    }

    @Override
    public List<DisFieldMapping> selectDisFieldMappingList(DisFieldMapping disFieldMapping)
    {
        return disFieldMappingMapper.selectDisFieldMappingList(disFieldMapping);
    }

    @Override
    public List<DisFieldMapping> selectEnabledByInterfaceId(Long interfaceId)
    {
        return disFieldMappingMapper.selectEnabledByInterfaceId(interfaceId);
    }

    @Transactional
    @Override
    public int insertDisFieldMapping(DisFieldMapping disFieldMapping)
    {
        disFieldMapping.setCreateTime(DateUtils.getNowDate());
        return disFieldMappingMapper.insertDisFieldMapping(disFieldMapping);
    }

    @Transactional
    @Override
    public int updateDisFieldMapping(DisFieldMapping disFieldMapping)
    {
        disFieldMapping.setUpdateTime(DateUtils.getNowDate());
        return disFieldMappingMapper.updateDisFieldMapping(disFieldMapping);
    }

    @Transactional
    @Override
    public int deleteDisFieldMappingByMappingId(Long mappingId)
    {
        return disFieldMappingMapper.deleteDisFieldMappingByMappingId(mappingId);
    }

    @Transactional
    @Override
    public int deleteDisFieldMappingByMappingIds(Long[] mappingIds)
    {
        return disFieldMappingMapper.deleteDisFieldMappingByMappingIds(mappingIds);
    }
}
