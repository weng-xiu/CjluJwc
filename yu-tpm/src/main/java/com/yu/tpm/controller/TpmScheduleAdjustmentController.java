package com.yu.tpm.controller;

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
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 调停课申请Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/adjust")
public class TpmScheduleAdjustmentController extends BaseController
{
    @Autowired
    private ITpmScheduleAdjustmentService tpmScheduleAdjustmentService;

    @PreAuthorize("@ss.hasPermi('tpm:adjust:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        startPage();
        List<TpmScheduleAdjustment> list = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:adjust:export')")
    @Log(title = "调停课管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        List<TpmScheduleAdjustment> list = tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
        ExcelUtil<TpmScheduleAdjustment> util = new ExcelUtil<TpmScheduleAdjustment>(TpmScheduleAdjustment.class);
        util.exportExcel(response, list, "调停课管理数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:adjust:query')")
    @GetMapping(value = "/{adjustId}")
    public AjaxResult getInfo(@PathVariable("adjustId") Long adjustId)
    {
        return success(tpmScheduleAdjustmentService.selectTpmScheduleAdjustmentByAdjustId(adjustId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:adjust:add')")
    @Log(title = "调停课管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return toAjax(tpmScheduleAdjustmentService.insertTpmScheduleAdjustment(tpmScheduleAdjustment));
    }

    @PreAuthorize("@ss.hasPermi('tpm:adjust:edit')")
    @Log(title = "调停课管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return toAjax(tpmScheduleAdjustmentService.updateTpmScheduleAdjustment(tpmScheduleAdjustment));
    }

    @PreAuthorize("@ss.hasPermi('tpm:adjust:remove')")
    @Log(title = "调停课管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{adjustIds}")
    public AjaxResult remove(@PathVariable Long[] adjustIds)
    {
        return toAjax(tpmScheduleAdjustmentService.deleteTpmScheduleAdjustmentByAdjustIds(adjustIds));
    }
}
