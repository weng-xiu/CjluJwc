package com.yu.portal.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.yu.portal.domain.PortalNotice;
import com.yu.portal.service.IPortalNoticeService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 教务通知Controller（后台管理）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/noticeManage")
public class PortalNoticeController extends BaseController
{
    @Autowired
    private IPortalNoticeService portalNoticeService;

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalNotice portalNotice)
    {
        startPage();
        List<PortalNotice> list = portalNoticeService.selectPortalNoticeList(portalNotice);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:export')")
    @Log(title = "教务通知", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalNotice portalNotice)
    {
        List<PortalNotice> list = portalNoticeService.selectPortalNoticeList(portalNotice);
        ExcelUtil<PortalNotice> util = new ExcelUtil<PortalNotice>(PortalNotice.class);
        util.exportExcel(response, list, "教务通知数据");
    }

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable("noticeId") Long noticeId)
    {
        return success(portalNoticeService.selectPortalNoticeByNoticeId(noticeId));
    }

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:add')")
    @Log(title = "教务通知", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalNotice portalNotice)
    {
        return toAjax(portalNoticeService.insertPortalNotice(portalNotice));
    }

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:edit')")
    @Log(title = "教务通知", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalNotice portalNotice)
    {
        return toAjax(portalNoticeService.updatePortalNotice(portalNotice));
    }

    @PreAuthorize("@ss.hasPermi('portal:noticeManage:remove')")
    @Log(title = "教务通知", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        return toAjax(portalNoticeService.deletePortalNoticeByNoticeIds(noticeIds));
    }
}
