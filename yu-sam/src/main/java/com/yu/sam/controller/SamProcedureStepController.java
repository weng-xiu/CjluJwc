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
import com.yu.sam.domain.SamProcedureStep;
import com.yu.sam.service.ISamProcedureStepService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 离校环节配置Controller（S7c）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@RestController
@RequestMapping("/sam/procedureStep")
public class SamProcedureStepController extends BaseController
{
    @Autowired
    private ISamProcedureStepService samProcedureStepService;

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamProcedureStep samProcedureStep)
    {
        startPage();
        List<SamProcedureStep> list = samProcedureStepService.selectSamProcedureStepList(samProcedureStep);
        return getDataTable(list);
    }

    /** 全部环节（不分页，供办理对话框加载） */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:list')")
    @GetMapping("/all")
    public AjaxResult all()
    {
        return success(samProcedureStepService.selectSamProcedureStepList(new SamProcedureStep()));
    }

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:export')")
    @Log(title = "离校环节配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamProcedureStep samProcedureStep)
    {
        List<SamProcedureStep> list = samProcedureStepService.selectSamProcedureStepList(samProcedureStep);
        ExcelUtil<SamProcedureStep> util = new ExcelUtil<SamProcedureStep>(SamProcedureStep.class);
        util.exportExcel(response, list, "离校环节配置数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:query')")
    @GetMapping(value = "/{stepId}")
    public AjaxResult getInfo(@PathVariable("stepId") Long stepId)
    {
        return success(samProcedureStepService.selectSamProcedureStepByStepId(stepId));
    }

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:add')")
    @Log(title = "离校环节配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamProcedureStep samProcedureStep)
    {
        samProcedureStep.setCreateBy(getUsername());
        return toAjax(samProcedureStepService.insertSamProcedureStep(samProcedureStep));
    }

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:edit')")
    @Log(title = "离校环节配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamProcedureStep samProcedureStep)
    {
        samProcedureStep.setUpdateBy(getUsername());
        return toAjax(samProcedureStepService.updateSamProcedureStep(samProcedureStep));
    }

    @PreAuthorize("@ss.hasPermi('sam:procedureStep:remove')")
    @Log(title = "离校环节配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{stepIds}")
    public AjaxResult remove(@PathVariable Long[] stepIds)
    {
        return toAjax(samProcedureStepService.deleteSamProcedureStepByStepIds(stepIds));
    }
}
