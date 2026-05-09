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
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.service.ITpmTrainingPlanService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 培养方案Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/plan")
public class TpmTrainingPlanController extends BaseController
{
    @Autowired
    private ITpmTrainingPlanService tpmTrainingPlanService;

    @PreAuthorize("@ss.hasPermi('tpm:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmTrainingPlan tpmTrainingPlan)
    {
        startPage();
        List<TpmTrainingPlan> list = tpmTrainingPlanService.selectTpmTrainingPlanList(tpmTrainingPlan);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:export')")
    @Log(title = "培养方案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmTrainingPlan tpmTrainingPlan)
    {
        List<TpmTrainingPlan> list = tpmTrainingPlanService.selectTpmTrainingPlanList(tpmTrainingPlan);
        ExcelUtil<TpmTrainingPlan> util = new ExcelUtil<TpmTrainingPlan>(TpmTrainingPlan.class);
        util.exportExcel(response, list, "培养方案数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable("planId") Long planId)
    {
        return success(tpmTrainingPlanService.selectTpmTrainingPlanByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:add')")
    @Log(title = "培养方案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TpmTrainingPlan tpmTrainingPlan)
    {
        return toAjax(tpmTrainingPlanService.insertTpmTrainingPlan(tpmTrainingPlan));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:edit')")
    @Log(title = "培养方案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TpmTrainingPlan tpmTrainingPlan)
    {
        return toAjax(tpmTrainingPlanService.updateTpmTrainingPlan(tpmTrainingPlan));
    }

    @PreAuthorize("@ss.hasPermi('tpm:plan:remove')")
    @Log(title = "培养方案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(tpmTrainingPlanService.deleteTpmTrainingPlanByPlanIds(planIds));
    }
}
