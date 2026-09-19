package com.yu.sam.controller;

import java.util.List;
import java.util.Map;
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
import com.yu.sam.domain.SamGraduationReview;
import com.yu.sam.service.ISamGraduationReviewService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/graduationReview")
public class SamGraduationReviewController extends BaseController
{
    @Autowired
    private ISamGraduationReviewService samGraduationReviewService;

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamGraduationReview samGraduationReview) { startPage(); List<SamGraduationReview> list = samGraduationReviewService.selectSamGraduationReviewList(samGraduationReview); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:export')")
    @Log(title = "毕业审核", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamGraduationReview samGraduationReview) { List<SamGraduationReview> list = samGraduationReviewService.selectSamGraduationReviewList(samGraduationReview); ExcelUtil<SamGraduationReview> util = new ExcelUtil<SamGraduationReview>(SamGraduationReview.class); util.exportExcel(response, list, "毕业审核数据"); }

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:query')")
    @GetMapping(value = "/{reviewId}")
    public AjaxResult getInfo(@PathVariable("reviewId") Long reviewId) { return success(samGraduationReviewService.selectSamGraduationReviewByReviewId(reviewId)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:add')")
    @Log(title = "毕业审核", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamGraduationReview samGraduationReview) { return toAjax(samGraduationReviewService.insertSamGraduationReview(samGraduationReview)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:edit')")
    @Log(title = "毕业审核", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamGraduationReview samGraduationReview) { return toAjax(samGraduationReviewService.updateSamGraduationReview(samGraduationReview)); }

    @PreAuthorize("@ss.hasPermi('sam:graduationReview:remove')")
    @Log(title = "毕业审核", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reviewIds}")
    public AjaxResult remove(@PathVariable Long[] reviewIds) { return toAjax(samGraduationReviewService.deleteSamGraduationReviewByReviewIds(reviewIds)); }

    /** S1：单人自动审核 */
    @PreAuthorize("@ss.hasPermi('sam:graduationReview:audit')")
    @Log(title = "毕业自动审核", businessType = BusinessType.INSERT)
    @PostMapping("/autoReview/{studentId}")
    public AjaxResult autoReview(@PathVariable("studentId") Long studentId) {
        return success(samGraduationReviewService.autoReview(studentId));
    }

    /** S1：批量自动审核（传 studentIds） */
    @PreAuthorize("@ss.hasPermi('sam:graduationReview:audit')")
    @Log(title = "毕业批量审核", businessType = BusinessType.INSERT)
    @PostMapping("/batchReview")
    public AjaxResult batchReview(@RequestBody List<Long> studentIds) {
        Map<String, Object> result = samGraduationReviewService.batchAutoReview(studentIds);
        return success(result);
    }
}
