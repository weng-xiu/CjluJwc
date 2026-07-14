package com.yu.oa.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;
import com.yu.oa.domain.OaNotice;
import com.yu.oa.service.IOaNoticeService;

/**
 * 通知公告Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/notice")
public class OaNoticeController extends BaseController
{
    @Autowired
    private IOaNoticeService oaNoticeService;

    /**
     * 查询通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaNotice oaNotice)
    {
        startPage();
        List<OaNotice> list = oaNoticeService.selectOaNoticeList(oaNotice);
        return getDataTable(list);
    }

    /**
     * 导出通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:export')")
    @Log(title = "通知公告", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaNotice oaNotice)
    {
        List<OaNotice> list = oaNoticeService.selectOaNoticeList(oaNotice);
        ExcelUtil<OaNotice> util = new ExcelUtil<OaNotice>(OaNotice.class);
        util.exportExcel(response, list, "通知公告数据");
    }

    /**
     * 获取通知公告详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:query')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable("noticeId") Long noticeId)
    {
        return success(oaNoticeService.selectOaNoticeByNoticeId(noticeId));
    }

    /**
     * 新增通知公告
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaNotice oaNotice)
    {
        return toAjax(oaNoticeService.insertOaNotice(oaNotice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaNotice oaNotice)
    {
        return toAjax(oaNoticeService.updateOaNotice(oaNotice));
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        return toAjax(oaNoticeService.deleteOaNoticeByNoticeIds(noticeIds));
    }

    /**
     * 发布公告
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:publish')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PostMapping("/publish/{noticeId}")
    public AjaxResult publish(@PathVariable("noticeId") Long noticeId)
    {
        return toAjax(oaNoticeService.publishOaNotice(noticeId));
    }

    /**
     * 撤回公告
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PostMapping("/revoke/{noticeId}")
    public AjaxResult revoke(@PathVariable("noticeId") Long noticeId)
    {
        return toAjax(oaNoticeService.revokeOaNotice(noticeId));
    }

    /**
     * 已读回执
     */
    @PreAuthorize("@ss.hasPermi('oa:notice:query')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PostMapping("/read/{noticeId}")
    public AjaxResult read(@PathVariable("noticeId") Long noticeId)
    {
        return toAjax(oaNoticeService.readOaNotice(noticeId));
    }
}
