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
import com.yu.oa.domain.OaSchedule;
import com.yu.oa.service.IOaScheduleService;

/**
 * 日程安排Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/oa/schedule")
public class OaScheduleController extends BaseController
{
    @Autowired
    private IOaScheduleService oaScheduleService;

    /**
     * 查询日程安排列表
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    @GetMapping("/list")
    public TableDataInfo list(OaSchedule oaSchedule)
    {
        startPage();
        List<OaSchedule> list = oaScheduleService.selectOaScheduleList(oaSchedule);
        return getDataTable(list);
    }

    /**
     * 导出日程安排列表
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:export')")
    @Log(title = "日程安排", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, OaSchedule oaSchedule)
    {
        List<OaSchedule> list = oaScheduleService.selectOaScheduleList(oaSchedule);
        ExcelUtil<OaSchedule> util = new ExcelUtil<OaSchedule>(OaSchedule.class);
        util.exportExcel(response, list, "日程安排数据");
    }

    /**
     * 获取日程安排详细信息
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:query')")
    @GetMapping(value = "/{scheduleId}")
    public AjaxResult getInfo(@PathVariable("scheduleId") Long scheduleId)
    {
        return success(oaScheduleService.selectOaScheduleByScheduleId(scheduleId));
    }

    /**
     * 新增日程安排
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:add')")
    @Log(title = "日程安排", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OaSchedule oaSchedule)
    {
        return toAjax(oaScheduleService.insertOaSchedule(oaSchedule));
    }

    /**
     * 修改日程安排
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:edit')")
    @Log(title = "日程安排", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OaSchedule oaSchedule)
    {
        return toAjax(oaScheduleService.updateOaSchedule(oaSchedule));
    }

    /**
     * 删除日程安排
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:remove')")
    @Log(title = "日程安排", businessType = BusinessType.DELETE)
    @DeleteMapping("/{scheduleIds}")
    public AjaxResult remove(@PathVariable Long[] scheduleIds)
    {
        return toAjax(oaScheduleService.deleteOaScheduleByScheduleIds(scheduleIds));
    }
}
