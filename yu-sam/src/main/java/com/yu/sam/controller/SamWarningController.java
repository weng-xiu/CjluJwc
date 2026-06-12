package com.yu.sam.controller;

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
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/warning")
public class SamWarningController extends BaseController
{
    @Autowired
    private ISamWarningService samWarningService;

    @PreAuthorize("@ss.hasPermi('sam:warning:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamWarning samWarning) { startPage(); List<SamWarning> list = samWarningService.selectSamWarningList(samWarning); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:warning:export')")
    @Log(title = "学籍预警", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamWarning samWarning) { List<SamWarning> list = samWarningService.selectSamWarningList(samWarning); ExcelUtil<SamWarning> util = new ExcelUtil<SamWarning>(SamWarning.class); util.exportExcel(response, list, "学籍预警数据"); }

    @PreAuthorize("@ss.hasPermi('sam:warning:query')")
    @GetMapping(value = "/{warningId}")
    public AjaxResult getInfo(@PathVariable("warningId") Long warningId) { return success(samWarningService.selectSamWarningByWarningId(warningId)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:add')")
    @Log(title = "学籍预警", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamWarning samWarning) { return toAjax(samWarningService.insertSamWarning(samWarning)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:edit')")
    @Log(title = "学籍预警", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamWarning samWarning) { return toAjax(samWarningService.updateSamWarning(samWarning)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:remove')")
    @Log(title = "学籍预警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{warningIds}")
    public AjaxResult remove(@PathVariable Long[] warningIds) { return toAjax(samWarningService.deleteSamWarningByWarningIds(warningIds)); }
}
