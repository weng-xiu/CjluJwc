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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.sam.domain.SamStudentIntervention;
import com.yu.sam.service.ISamStudentInterventionService;

/**
 * 学生干预闭环控制器（F4 预警-帮扶-效果回收）。
 *
 * @author F4
 */
@RestController
@RequestMapping("/sam/intervention")
public class SamStudentInterventionController extends BaseController
{
    @Autowired
    private ISamStudentInterventionService interventionService;

    @PreAuthorize("@ss.hasPermi('sam:intervention:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamStudentIntervention query)
    {
        startPage();
        return getDataTable(interventionService.selectInterventionList(query));
    }

    @PreAuthorize("@ss.hasPermi('sam:intervention:export')")
    @Log(title = "学生干预台账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamStudentIntervention query)
    {
        List<SamStudentIntervention> list = interventionService.selectInterventionList(query);
        ExcelUtil<SamStudentIntervention> util = new ExcelUtil<SamStudentIntervention>(SamStudentIntervention.class);
        util.exportExcel(response, list, "学生干预台账数据");
    }

    /** 效果预览：登记前按基线/效果学期试算 GPA 与及格率变化。 */
    @PreAuthorize("@ss.hasPermi('sam:intervention:query')")
    @GetMapping("/preview")
    public AjaxResult preview(@RequestParam("studentId") Long studentId,
            @RequestParam(value = "baselineSemesterId", required = false) Long baselineSemesterId,
            @RequestParam(value = "effectSemesterId", required = false) Long effectSemesterId)
    {
        return success(interventionService.previewEffect(studentId, baselineSemesterId, effectSemesterId));
    }

    @PreAuthorize("@ss.hasPermi('sam:intervention:query')")
    @GetMapping(value = "/{interventionId}")
    public AjaxResult getInfo(@PathVariable("interventionId") Long interventionId)
    {
        return success(interventionService.selectInterventionById(interventionId));
    }

    @PreAuthorize("@ss.hasPermi('sam:intervention:add')")
    @Log(title = "学生干预台账", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamStudentIntervention record)
    {
        record.setCreateBy(getUsername());
        return toAjax(interventionService.insertIntervention(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:intervention:edit')")
    @Log(title = "学生干预台账", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamStudentIntervention record)
    {
        record.setUpdateBy(getUsername());
        return toAjax(interventionService.updateIntervention(record));
    }

    @PreAuthorize("@ss.hasPermi('sam:intervention:remove')")
    @Log(title = "学生干预台账", businessType = BusinessType.DELETE)
    @DeleteMapping("/{interventionIds}")
    public AjaxResult remove(@PathVariable Long[] interventionIds)
    {
        return toAjax(interventionService.deleteInterventionByIds(interventionIds));
    }
}
