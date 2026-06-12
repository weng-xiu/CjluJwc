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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmScheduleService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 排课管理Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/schedule")
public class TpmScheduleController extends BaseController
{
    @Autowired
    private ITpmScheduleService tpmScheduleService;

    @PreAuthorize("@ss.hasPermi('tpm:schedule:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmSchedule tpmSchedule)
    {
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(tpmSchedule);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:schedule:export')")
    @Log(title = "排课管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmSchedule tpmSchedule)
    {
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(tpmSchedule);
        ExcelUtil<TpmSchedule> util = new ExcelUtil<TpmSchedule>(TpmSchedule.class);
        util.exportExcel(response, list, "排课管理数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:schedule:query')")
    @GetMapping(value = "/{scheduleId}")
    public AjaxResult getInfo(@PathVariable("scheduleId") Long scheduleId)
    {
        return success(tpmScheduleService.selectTpmScheduleByScheduleId(scheduleId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:schedule:add')")
    @Log(title = "排课管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmSchedule tpmSchedule)
    {
        return toAjax(tpmScheduleService.insertTpmSchedule(tpmSchedule));
    }

    @PreAuthorize("@ss.hasPermi('tpm:schedule:edit')")
    @Log(title = "排课管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmSchedule tpmSchedule)
    {
        return toAjax(tpmScheduleService.updateTpmSchedule(tpmSchedule));
    }

    @PreAuthorize("@ss.hasPermi('tpm:schedule:remove')")
    @Log(title = "排课管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{scheduleIds}")
    public AjaxResult remove(@PathVariable Long[] scheduleIds)
    {
        return toAjax(tpmScheduleService.deleteTpmScheduleByScheduleIds(scheduleIds));
    }
}
