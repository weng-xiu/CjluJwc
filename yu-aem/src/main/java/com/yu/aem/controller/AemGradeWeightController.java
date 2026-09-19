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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemGradeWeight;
import com.yu.aem.service.IAemGradeWeightService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 成绩权重配置Controller（A4）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@RestController
@RequestMapping("/aem/gradeWeight")
public class AemGradeWeightController extends BaseController
{
    @Autowired
    private IAemGradeWeightService aemGradeWeightService;

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGradeWeight aemGradeWeight)
    {
        startPage();
        List<AemGradeWeight> list = aemGradeWeightService.selectAemGradeWeightList(aemGradeWeight);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:export')")
    @Log(title = "成绩权重配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemGradeWeight aemGradeWeight)
    {
        List<AemGradeWeight> list = aemGradeWeightService.selectAemGradeWeightList(aemGradeWeight);
        ExcelUtil<AemGradeWeight> util = new ExcelUtil<AemGradeWeight>(AemGradeWeight.class);
        util.exportExcel(response, list, "成绩权重配置数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:query')")
    @GetMapping(value = "/{weightId}")
    public AjaxResult getInfo(@PathVariable("weightId") Long weightId)
    {
        return success(aemGradeWeightService.selectAemGradeWeightByWeightId(weightId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:add')")
    @Log(title = "成绩权重配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AemGradeWeight aemGradeWeight)
    {
        aemGradeWeight.setCreateBy(getUsername());
        return toAjax(aemGradeWeightService.insertAemGradeWeight(aemGradeWeight));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:edit')")
    @Log(title = "成绩权重配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AemGradeWeight aemGradeWeight)
    {
        aemGradeWeight.setUpdateBy(getUsername());
        return toAjax(aemGradeWeightService.updateAemGradeWeight(aemGradeWeight));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:remove')")
    @Log(title = "成绩权重配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{weightIds}")
    public AjaxResult remove(@PathVariable Long[] weightIds)
    {
        return toAjax(aemGradeWeightService.deleteAemGradeWeightByWeightIds(weightIds));
    }

    /** A4：预览某课程实际生效的权重（供前端/教师录入页提示） */
    @PreAuthorize("@ss.hasPermi('aem:gradeWeight:query')")
    @GetMapping("/effective/{courseId}")
    public AjaxResult effective(@PathVariable("courseId") Long courseId)
    {
        double[] ratios = aemGradeWeightService.resolveRatios(courseId);
        AjaxResult result = success();
        result.put("regularRatio", ratios[0]);
        result.put("examRatio", ratios[1]);
        return result;
    }
}
