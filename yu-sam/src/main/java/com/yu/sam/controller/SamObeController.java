package com.yu.sam.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.service.ISamObeAttainmentService;

/**
 * N1 OBE 毕业要求达成度控制器。
 *
 * @author N1
 */
@RestController
@RequestMapping("/sam/obe")
public class SamObeController extends BaseController
{
    @Autowired
    private ISamObeAttainmentService obeService;

    // ---------- 毕业要求 ----------
    @PreAuthorize("@ss.hasPermi('sam:obeRequirement:list')")
    @GetMapping("/requirement/list")
    public TableDataInfo requirementList(SamObeGraduationRequirement query)
    {
        startPage();
        return getDataTable(obeService.selectRequirementList(query));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeRequirement:query')")
    @GetMapping("/requirement/{grId}")
    public AjaxResult requirementGet(@PathVariable("grId") Long grId)
    {
        return success(obeService.selectRequirementByGrId(grId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeRequirement:add')")
    @Log(title = "OBE毕业要求", businessType = BusinessType.INSERT)
    @PostMapping("/requirement")
    public AjaxResult requirementAdd(@Validated @RequestBody SamObeGraduationRequirement record)
    {
        return toAjax(obeService.insertRequirement(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeRequirement:edit')")
    @Log(title = "OBE毕业要求", businessType = BusinessType.UPDATE)
    @PutMapping("/requirement")
    public AjaxResult requirementEdit(@Validated @RequestBody SamObeGraduationRequirement record)
    {
        return toAjax(obeService.updateRequirement(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeRequirement:remove')")
    @Log(title = "OBE毕业要求", businessType = BusinessType.DELETE)
    @DeleteMapping("/requirement/{grIds}")
    public AjaxResult requirementRemove(@PathVariable Long[] grIds)
    {
        return toAjax(obeService.deleteRequirementByGrIds(grIds));
    }

    // ---------- 指标点 ----------
    @PreAuthorize("@ss.hasPermi('sam:obeIndicator:list')")
    @GetMapping("/indicator/list")
    public TableDataInfo indicatorList(SamObeIndicatorPoint query)
    {
        startPage();
        return getDataTable(obeService.selectIndicatorList(query));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeIndicator:query')")
    @GetMapping("/indicator/{ipId}")
    public AjaxResult indicatorGet(@PathVariable("ipId") Long ipId)
    {
        return success(obeService.selectIndicatorByIpId(ipId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeIndicator:add')")
    @Log(title = "OBE指标点", businessType = BusinessType.INSERT)
    @PostMapping("/indicator")
    public AjaxResult indicatorAdd(@Validated @RequestBody SamObeIndicatorPoint record)
    {
        return toAjax(obeService.insertIndicator(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeIndicator:edit')")
    @Log(title = "OBE指标点", businessType = BusinessType.UPDATE)
    @PutMapping("/indicator")
    public AjaxResult indicatorEdit(@Validated @RequestBody SamObeIndicatorPoint record)
    {
        return toAjax(obeService.updateIndicator(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeIndicator:remove')")
    @Log(title = "OBE指标点", businessType = BusinessType.DELETE)
    @DeleteMapping("/indicator/{ipIds}")
    public AjaxResult indicatorRemove(@PathVariable Long[] ipIds)
    {
        return toAjax(obeService.deleteIndicatorByIpIds(ipIds));
    }

    // ---------- 课程支撑矩阵 ----------
    @PreAuthorize("@ss.hasPermi('sam:obeSupport:list')")
    @GetMapping("/support/list")
    public TableDataInfo supportList(SamObeCourseSupport query)
    {
        startPage();
        return getDataTable(obeService.selectSupportList(query));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeSupport:query')")
    @GetMapping("/support/{supportId}")
    public AjaxResult supportGet(@PathVariable("supportId") Long supportId)
    {
        return success(obeService.selectSupportBySupportId(supportId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeSupport:add')")
    @Log(title = "OBE课程支撑", businessType = BusinessType.INSERT)
    @PostMapping("/support")
    public AjaxResult supportAdd(@Validated @RequestBody SamObeCourseSupport record)
    {
        return toAjax(obeService.insertSupport(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeSupport:edit')")
    @Log(title = "OBE课程支撑", businessType = BusinessType.UPDATE)
    @PutMapping("/support")
    public AjaxResult supportEdit(@Validated @RequestBody SamObeCourseSupport record)
    {
        return toAjax(obeService.updateSupport(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeSupport:remove')")
    @Log(title = "OBE课程支撑", businessType = BusinessType.DELETE)
    @DeleteMapping("/support/{supportIds}")
    public AjaxResult supportRemove(@PathVariable Long[] supportIds)
    {
        return toAjax(obeService.deleteSupportBySupportIds(supportIds));
    }

    // ---------- 达成度计算 ----------
    @PreAuthorize("@ss.hasPermi('sam:obeAttainment:query')")
    @GetMapping("/calculate/student")
    public AjaxResult calculateByStudent(@RequestParam("planId") Long planId, @RequestParam("studentId") Long studentId)
    {
        return success(obeService.calculateByStudent(planId, studentId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeAttainment:query')")
    @GetMapping("/calculate/cohort")
    public AjaxResult calculateCohort(@RequestParam("planId") Long planId,
                                      @RequestParam(value = "semesterId", required = false) Long semesterId)
    {
        return success(obeService.calculateCohort(planId, semesterId));
    }

    @PreAuthorize("@ss.hasPermi('sam:obeAttainment:list')")
    @GetMapping("/dashboard")
    public AjaxResult dashboard(@RequestParam("planId") Long planId,
                                @RequestParam(value = "semesterId", required = false) Long semesterId)
    {
        return success(obeService.dashboard(planId, semesterId));
    }
}
