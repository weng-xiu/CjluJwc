package com.yu.portal.service;

import java.util.List;
import com.yu.portal.domain.PortalBanner;

/**
 * 门户轮播Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IPortalBannerService
{
    /**
     * 查询门户轮播列表
     *
     * @param portalBanner 门户轮播
     * @return 门户轮播集合
     */
    public List<PortalBanner> selectPortalBannerList(PortalBanner portalBanner);

    /**
     * 通过轮播ID查询门户轮播
     *
     * @param bannerId 轮播ID
     * @return 门户轮播
     */
    public PortalBanner selectPortalBannerById(Long bannerId);

    /**
     * 查询当前激活的轮播列表
     *
     * @return 门户轮播集合
     */
    public List<PortalBanner> selectActiveBanners();

    /**
     * 新增门户轮播
     *
     * @param portalBanner 门户轮播
     * @return 结果
     */
    public int insertPortalBanner(PortalBanner portalBanner);

    /**
     * 修改门户轮播
     *
     * @param portalBanner 门户轮播
     * @return 结果
     */
    public int updatePortalBanner(PortalBanner portalBanner);

    /**
     * 批量删除门户轮播
     *
     * @param bannerIds 轮播ID数组
     * @return 结果
     */
    public int deletePortalBannerByIds(Long[] bannerIds);
}
