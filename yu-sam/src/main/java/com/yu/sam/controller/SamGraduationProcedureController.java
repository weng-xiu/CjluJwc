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
import org.springframework.web.bind.annotation.RequestParam;
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

    /** S7c：初始化离校手续与环节明细（幂等，studentId 为空则全部已毕业生） */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:init')")
    @Log(title = "离校手续", businessType = BusinessType.INSERT)
    @PostMapping("/init")
    public AjaxResult init(@RequestParam(required = false) Long studentId)
    {
        int rows = samGraduationProcedureService.initProcedures(studentId, getUsername());
        return AjaxResult.success("初始化完成，新增 " + rows + " 条离校手续记录", rows);
    }

    /** S7c：某手续的环节办理明细 */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:query')")
    @GetMapping("/items/{procedureId}")
    public AjaxResult items(@PathVariable("procedureId") Long procedureId)
    {
        return success(samGraduationProcedureService.listItems(procedureId));
    }

    /** S7c：人工勾选/取消某环节办理（done=true/false），自动重算手续状态 */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:handle')")
    @Log(title = "离校手续", businessType = BusinessType.UPDATE)
    @PutMapping("/toggle")
    public AjaxResult toggle(@RequestBody ToggleForm form)
    {
        return toAjax(samGraduationProcedureService.toggleItem(form.getProcedureId(), form.getStepId(), Boolean.TRUE.equals(form.getDone()), getUsername()));
    }

    /** S7c：自动判定（按环节数据源同步办理状态），studentId 为空则全部 */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:autoCheck')")
    @Log(title = "离校手续", businessType = BusinessType.UPDATE)
    @PostMapping("/autoCheck")
    public AjaxResult autoCheck(@RequestParam(required = false) Long studentId)
    {
        int rows = samGraduationProcedureService.autoCheck(studentId, getUsername());
        return AjaxResult.success("自动判定完成，更新 " + rows + " 名学生离校状态", rows);
    }

    /** S7c：离校办理总览统计 */
    @PreAuthorize("@ss.hasPermi('sam:graduationProcedure:list')")
    @GetMapping("/stat/overview")
    public AjaxResult statOverview()
    {
        return success(samGraduationProcedureService.statOverview());
    }

    /** 环节办理表单 */
    public static class ToggleForm
    {
        private Long procedureId;
        private Long stepId;
        private Boolean done;
        public Long getProcedureId() { return procedureId; }
        public void setProcedureId(Long procedureId) { this.procedureId = procedureId; }
        public Long getStepId() { return stepId; }
        public void setStepId(Long stepId) { this.stepId = stepId; }
        public Boolean getDone() { return done; }
        public void setDone(Boolean done) { this.done = done; }
    }
}
