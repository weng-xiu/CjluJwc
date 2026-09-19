package com.yu.sam.controller;

import java.util.List;
import java.util.Map;
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

    /**
     * 提交异动申请并启动多级审批流程
     */
    @PreAuthorize("@ss.hasPermi('sam:statusChange:edit')")
    @Log(title = "学籍异动-提交审批", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{changeId}")
    public AjaxResult submit(@PathVariable("changeId") Long changeId)
    {
        return toAjax(samStatusChangeService.submitForApproval(changeId));
    }

    /**
     * 审批通过（回写学籍状态+联动）
     */
    @PreAuthorize("@ss.hasPermi('sam:statusChange:approve')")
    @Log(title = "学籍异动-审批通过", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{changeId}")
    public AjaxResult approve(@PathVariable("changeId") Long changeId, @RequestBody Map<String, String> params)
    {
        String taskId = params.get("taskId");
        String comment = params.get("comment");
        return toAjax(samStatusChangeService.approveChange(changeId, taskId, comment));
    }

    /**
     * 驳回异动申请
     */
    @PreAuthorize("@ss.hasPermi('sam:statusChange:approve')")
    @Log(title = "学籍异动-驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{changeId}")
    public AjaxResult reject(@PathVariable("changeId") Long changeId, @RequestBody Map<String, String> params)
    {
        String taskId = params.get("taskId");
        String comment = params.get("comment");
        return toAjax(samStatusChangeService.rejectChange(changeId, taskId, comment));
    }
}
