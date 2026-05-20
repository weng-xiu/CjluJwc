package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.page.TableDataInfo;
import com.yu.portal.domain.PortalNotice;
import com.yu.portal.service.IPortalNoticeService;

/**
 * 教务通知前台Controller（学生/教师查看通知）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/notice")
public class PortalNoticeFrontController extends BaseController
{
    @Autowired
    private IPortalNoticeService portalNoticeService;

    @PreAuthorize("@ss.hasPermi('portal:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalNotice portalNotice)
    {
        startPage();
        // 仅查询已发布的通知
        portalNotice.setPublishStatus("1");
        List<PortalNotice> list = portalNoticeService.selectPortalNoticeList(portalNotice);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('portal:notice:query')")
    @GetMapping(value = "/detail")
    public AjaxResult detail(PortalNotice portalNotice)
    {
        return success(portalNoticeService.selectPortalNoticeList(portalNotice));
    }
}
