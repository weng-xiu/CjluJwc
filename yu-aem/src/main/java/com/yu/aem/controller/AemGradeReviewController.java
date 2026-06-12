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
import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.service.IAemGradeReviewService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 成绩复核审批Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/gradeReview")
public class AemGradeReviewController extends BaseController
{
    @Autowired
    private IAemGradeReviewService aemGradeReviewService;

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGradeReview aemGradeReview)
    {
        startPage();
        List<AemGradeReview> list = aemGradeReviewService.selectAemGradeReviewList(aemGradeReview);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:export')")
    @Log(title = "成绩复核审批", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemGradeReview aemGradeReview)
    {
        List<AemGradeReview> list = aemGradeReviewService.selectAemGradeReviewList(aemGradeReview);
        ExcelUtil<AemGradeReview> util = new ExcelUtil<AemGradeReview>(AemGradeReview.class);
        util.exportExcel(response, list, "成绩复核审批数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:query')")
    @GetMapping(value = "/{reviewId}")
    public AjaxResult getInfo(@PathVariable("reviewId") Long reviewId)
    {
        return success(aemGradeReviewService.selectAemGradeReviewByReviewId(reviewId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:add')")
    @Log(title = "成绩复核审批", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemGradeReview aemGradeReview)
    {
        return toAjax(aemGradeReviewService.insertAemGradeReview(aemGradeReview));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:edit')")
    @Log(title = "成绩复核审批", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemGradeReview aemGradeReview)
    {
        return toAjax(aemGradeReviewService.updateAemGradeReview(aemGradeReview));
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeReview:remove')")
    @Log(title = "成绩复核审批", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reviewIds}")
    public AjaxResult remove(@PathVariable Long[] reviewIds)
    {
        return toAjax(aemGradeReviewService.deleteAemGradeReviewByReviewIds(reviewIds));
    }
}
