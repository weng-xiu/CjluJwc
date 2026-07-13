package com.yu.portal.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.portal.mapper.PortalColumnMapper;
import com.yu.portal.domain.PortalColumn;
import com.yu.portal.service.IPortalColumnService;

/**
 * 门户栏目Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class PortalColumnServiceImpl implements IPortalColumnService
{
    @Autowired
    private PortalColumnMapper portalColumnMapper;

    @Override
    public List<PortalColumn> selectPortalColumnList(PortalColumn portalColumn)
    {
        return portalColumnMapper.selectPortalColumnList(portalColumn);
    }

    @Override
    public PortalColumn selectPortalColumnById(Long columnId)
    {
        return portalColumnMapper.selectPortalColumnById(columnId);
    }

    @Override
    public PortalColumn selectPortalColumnByCode(String columnCode)
    {
        return portalColumnMapper.selectPortalColumnByCode(columnCode);
    }

    @Transactional
    @Override
    public int insertPortalColumn(PortalColumn portalColumn)
    {
        portalColumn.setCreateTime(DateUtils.getNowDate());
        return portalColumnMapper.insertPortalColumn(portalColumn);
    }

    @Transactional
    @Override
    public int updatePortalColumn(PortalColumn portalColumn)
    {
        portalColumn.setUpdateTime(DateUtils.getNowDate());
        return portalColumnMapper.updatePortalColumn(portalColumn);
    }

    @Transactional
    @Override
    public int deletePortalColumnByIds(Long[] columnIds)
    {
        return portalColumnMapper.deletePortalColumnByIds(columnIds);
    }
}
