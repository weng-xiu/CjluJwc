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
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.service.ISamStatusChangeService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/statusChange")
public class SamStatusChangeController extends BaseController
{
    @Autowired
    private ISamStatusChangeService samStatusChangeService;

    @PreAuthorize("@ss.hasPermi('sam:statusChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamStatusChange samStatusChange) { startPage(); List<SamStatusChange> list = samStatusChangeService.selectSamStatusChangeList(samStatusChange); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:statusChange:export')")
    @Log(title = "学籍异动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamStatusChange samStatusChange) { List<SamStatusChange> list = samStatusChangeService.selectSamStatusChangeList(samStatusChange); ExcelUtil<SamStatusChange> util = new ExcelUtil<SamStatusChange>(SamStatusChange.class); util.exportExcel(response, list, "学籍异动数据"); }

    @PreAuthorize("@ss.hasPermi('sam:statusChange:query')")
    @GetMapping(value = "/{changeId}")
    public AjaxResult getInfo(@PathVariable("changeId") Long changeId) { return success(samStatusChangeService.selectSamStatusChangeByChangeId(changeId)); }

    @PreAuthorize("@ss.hasPermi('sam:statusChange:add')")
    @Log(title = "学籍异动", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamStatusChange samStatusChange) { return toAjax(samStatusChangeService.insertSamStatusChange(samStatusChange)); }

    @PreAuthorize("@ss.hasPermi('sam:statusChange:edit')")
    @Log(title = "学籍异动", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamStatusChange samStatusChange) { return toAjax(samStatusChangeService.updateSamStatusChange(samStatusChange)); }

    @PreAuthorize("@ss.hasPermi('sam:statusChange:remove')")
    @Log(title = "学籍异动", businessType = BusinessType.DELETE)
    @DeleteMapping("/{changeIds}")
    public AjaxResult remove(@PathVariable Long[] changeIds) { return toAjax(samStatusChangeService.deleteSamStatusChangeByChangeIds(changeIds)); }
}
