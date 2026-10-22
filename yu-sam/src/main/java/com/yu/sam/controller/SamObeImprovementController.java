package com.yu.sam.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.sam.domain.SamObeImprovement;
import com.yu.sam.service.ISamObeImprovementService;

/**
 * N1 OBE 持续改进台账控制器（F3 评价-反馈-改进闭环）。
 *
 * @author N1
 */
@RestController
@RequestMapping("/sam/obe/improvement")
public class SamObeImprovementController extends BaseController
{
    @Autowired
    private ISamObeImprovementService improvementService;

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamObeImprovement query)
    {
        startPage();
        return getDataTable(improvementService.selectImprovementList(query));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:export')")
    @Log(title = "OBE改进台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamObeImprovement query)
    {
        List<SamObeImprovement> list = improvementService.selectImprovementList(query);
        ExcelUtil<SamObeImprovement> util = new ExcelUtil<SamObeImprovement>(SamObeImprovement.class);
        util.exportExcel(response, list, "持续改进台账数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:query')")
    @GetMapping(value = "/{impId}")
    public AjaxResult getInfo(@PathVariable("impId") Long impId)
    {
        return success(improvementService.selectImprovementByImpId(impId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:add')")
    @Log(title = "OBE改进台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamObeImprovement record)
    {
        record.setCreateBy(getUsername());
        return toAjax(improvementService.insertImprovement(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:edit')")
    @Log(title = "OBE改进台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamObeImprovement record)
    {
        record.setUpdateBy(getUsername());
        return toAjax(improvementService.updateImprovement(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeImprovement:remove')")
    @Log(title = "OBE改进台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{impIds}")
    public AjaxResult remove(@PathVariable Long[] impIds)
    {
        return toAjax(improvementService.deleteImprovementByImpIds(impIds));
    }
}
