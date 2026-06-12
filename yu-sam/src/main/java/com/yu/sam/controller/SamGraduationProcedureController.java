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
import com.yu.sam.domain.SamGraduationProcedure;
import com.yu.sam.service.ISamGraduationProcedureService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/graduationProcedure")
public class SamGraduationProcedureController extends BaseController
{
    @Autowired
    private ISamGraduationProcedureService samGraduationProcedureService;

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamGraduationProcedure samGraduationProcedure) { startPage(); List<SamGraduationProcedure> list = samGraduationProcedureService.selectSamGraduationProcedureList(samGraduationProcedure); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:export')")
    @Log(title = "离校手续", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamGraduationProcedure samGraduationProcedure) { List<SamGraduationProcedure> list = samGraduationProcedureService.selectSamGraduationProcedureList(samGraduationProcedure); ExcelUtil<SamGraduationProcedure> util = new ExcelUtil<SamGraduationProcedure>(SamGraduationProcedure.class); util.exportExcel(response, list, "离校手续数据"); }

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:query')")
    @GetMapping(value = "/{procedureId}")
    public AjaxResult getInfo(@PathVariable("procedureId") Long procedureId) { return success(samGraduationProcedureService.selectSamGraduationProcedureByProcedureId(procedureId)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:add')")
    @Log(title = "离校手续", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamGraduationProcedure samGraduationProcedure) { return toAjax(samGraduationProcedureService.insertSamGraduationProcedure(samGraduationProcedure)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:edit')")
    @Log(title = "离校手续", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamGraduationProcedure samGraduationProcedure) { return toAjax(samGraduationProcedureService.updateSamGraduationProcedure(samGraduationProcedure)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:remove')")
    @Log(title = "离校手续", businessType = BusinessType.DELETE)
    @DeleteMapping("/{procedureIds}")
    public AjaxResult remove(@PathVariable Long[] procedureIds) { return toAjax(samGraduationProcedureService.deleteSamGraduationProcedureByProcedureIds(procedureIds)); }
}
