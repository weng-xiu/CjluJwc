package com.yu.dis.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.dis.mapper.DisExternalSystemMapper;
import com.yu.dis.domain.DisExternalSystem;
import com.yu.dis.service.IDisExternalSystemService;

/**
 * 外部系统配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
@Service
public class DisExternalSystemServiceImpl implements IDisExternalSystemService 
{
    @Autowired
    private DisExternalSystemMapper disExternalSystemMapper;

    @Override
    public DisExternalSystem selectDisExternalSystemBySystemId(Long systemId)
    {
        return disExternalSystemMapper.selectDisExternalSystemBySystemId(systemId);
    }

    @Override
    public List<DisExternalSystem> selectDisExternalSystemList(DisExternalSystem disExternalSystem)
    {
        return disExternalSystemMapper.selectDisExternalSystemList(disExternalSystem);
    }

    @Override
    public int insertDisExternalSystem(DisExternalSystem disExternalSystem)
    {
        disExternalSystem.setCreateTime(DateUtils.getNowDate());
        return disExternalSystemMapper.insertDisExternalSystem(disExternalSystem);
    }

    @Override
    public int updateDisExternalSystem(DisExternalSystem disExternalSystem)
    {
        disExternalSystem.setUpdateTime(DateUtils.getNowDate());
        return disExternalSystemMapper.updateDisExternalSystem(disExternalSystem);
    }

    @Override
    public int deleteDisExternalSystemBySystemId(Long systemId)
    {
        return disExternalSystemMapper.deleteDisExternalSystemBySystemId(systemId);
    }

    @Override
    public int deleteDisExternalSystemBySystemIds(Long[] systemIds)
    {
        return disExternalSystemMapper.deleteDisExternalSystemBySystemIds(systemIds);
    }
}
