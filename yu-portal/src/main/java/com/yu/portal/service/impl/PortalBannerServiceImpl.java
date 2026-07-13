package com.yu.portal.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.portal.mapper.PortalBannerMapper;
import com.yu.portal.domain.PortalBanner;
import com.yu.portal.service.IPortalBannerService;

/**
 * 门户轮播Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class PortalBannerServiceImpl implements IPortalBannerService
{
    @Autowired
    private PortalBannerMapper portalBannerMapper;

    @Override
    public List<PortalBanner> selectPortalBannerList(PortalBanner portalBanner)
    {
        return portalBannerMapper.selectPortalBannerList(portalBanner);
    }

    @Override
    public PortalBanner selectPortalBannerById(Long bannerId)
    {
        return portalBannerMapper.selectPortalBannerById(bannerId);
    }

    @Override
    public List<PortalBanner> selectActiveBanners()
    {
        return portalBannerMapper.selectActiveBanners();
    }

    @Transactional
    @Override
    public int insertPortalBanner(PortalBanner portalBanner)
    {
        portalBanner.setCreateTime(DateUtils.getNowDate());
        return portalBannerMapper.insertPortalBanner(portalBanner);
    }

    @Transactional
    @Override
    public int updatePortalBanner(PortalBanner portalBanner)
    {
        portalBanner.setUpdateTime(DateUtils.getNowDate());
        return portalBannerMapper.updatePortalBanner(portalBanner);
    }

    @Transactional
    @Override
    public int deletePortalBannerByIds(Long[] bannerIds)
    {
        return portalBannerMapper.deletePortalBannerByIds(bannerIds);
    }
}
