package com.yu.aem.controller;

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
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.service.IAemExamPlanService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 考试安排Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/examPlan")
public class AemExamPlanController extends BaseController
{
    @Autowired
    private IAemExamPlanService aemExamPlanService;

    @PreAuthorize("@ss.hasPermi('aem:examPlan:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemExamPlan aemExamPlan)
    {
        startPage();
        List<AemExamPlan> list = aemExamPlanService.selectAemExamPlanList(aemExamPlan);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:examPlan:export')")
    @Log(title = "考试安排", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemExamPlan aemExamPlan)
    {
        List<AemExamPlan> list = aemExamPlanService.selectAemExamPlanList(aemExamPlan);
        ExcelUtil<AemExamPlan> util = new ExcelUtil<AemExamPlan>(AemExamPlan.class);
        util.exportExcel(response, list, "考试安排数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:examPlan:query')")
    @GetMapping(value = "/{examId}")
    public AjaxResult getInfo(@PathVariable("examId") Long examId)
    {
        return success(aemExamPlanService.selectAemExamPlanByExamId(examId));
    }

    /**
     * 查询考试安排明细（含座位、监考子表），用于主子表联动展开
     */
    @PreAuthorize("@ss.hasPermi('aem:examPlan:query')")
    @GetMapping(value = "/detail/{examId}")
    public AjaxResult getDetail(@PathVariable("examId") Long examId)
    {
        return success(aemExamPlanService.selectAemExamPlanDetail(examId));
    }

    @PreAuthorize("@ss.hasPermi('aem:examPlan:add')")
    @Log(title = "考试安排", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemExamPlan aemExamPlan)
    {
        return toAjax(aemExamPlanService.insertAemExamPlan(aemExamPlan));
    }

    @PreAuthorize("@ss.hasPermi('aem:examPlan:edit')")
    @Log(title = "考试安排", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemExamPlan aemExamPlan)
    {
        return toAjax(aemExamPlanService.updateAemExamPlan(aemExamPlan));
    }

    @PreAuthorize("@ss.hasPermi('aem:examPlan:remove')")
    @Log(title = "考试安排", businessType = BusinessType.DELETE)
    @DeleteMapping("/{examIds}")
    public AjaxResult remove(@PathVariable Long[] examIds)
    {
        return toAjax(aemExamPlanService.deleteAemExamPlanByExamIds(examIds));
    }
}
