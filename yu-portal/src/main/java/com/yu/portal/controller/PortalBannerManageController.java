package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.portal.domain.PortalBanner;
import com.yu.portal.service.IPortalBannerService;
import com.yu.common.core.page.TableDataInfo;

/**
 * 门户轮播管理Controller（后台管理）
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/portal/bannerManage")
public class PortalBannerManageController extends BaseController
{
    @Autowired
    private IPortalBannerService portalBannerService;

    @PreAuthorize("@ss.hasPermi('portal:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalBanner portalBanner)
    {
        startPage();
        List<PortalBanner> list = portalBannerService.selectPortalBannerList(portalBanner);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('portal:banner:query')")
    @GetMapping(value = "/{bannerId}")
    public AjaxResult getInfo(@PathVariable("bannerId") Long bannerId)
    {
        return success(portalBannerService.selectPortalBannerById(bannerId));
    }

    @PreAuthorize("@ss.hasPermi('portal:banner:add')")
    @Log(title = "门户轮播", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalBanner portalBanner)
    {
        return toAjax(portalBannerService.insertPortalBanner(portalBanner));
    }

    @PreAuthorize("@ss.hasPermi('portal:banner:edit')")
    @Log(title = "门户轮播", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalBanner portalBanner)
    {
        return toAjax(portalBannerService.updatePortalBanner(portalBanner));
    }

    @PreAuthorize("@ss.hasPermi('portal:banner:remove')")
    @Log(title = "门户轮播", businessType = BusinessType.DELETE)
    @DeleteMapping("/{bannerIds}")
    public AjaxResult remove(@PathVariable Long[] bannerIds)
    {
        return toAjax(portalBannerService.deletePortalBannerByIds(bannerIds));
    }
}
