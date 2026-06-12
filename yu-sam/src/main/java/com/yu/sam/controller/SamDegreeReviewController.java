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
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.service.ISamDegreeReviewService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/degreeReview")
public class SamDegreeReviewController extends BaseController
{
    @Autowired
    private ISamDegreeReviewService samDegreeReviewService;

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamDegreeReview samDegreeReview) { startPage(); List<SamDegreeReview> list = samDegreeReviewService.selectSamDegreeReviewList(samDegreeReview); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:export')")
    @Log(title = "学位审核", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamDegreeReview samDegreeReview) { List<SamDegreeReview> list = samDegreeReviewService.selectSamDegreeReviewList(samDegreeReview); ExcelUtil<SamDegreeReview> util = new ExcelUtil<SamDegreeReview>(SamDegreeReview.class); util.exportExcel(response, list, "学位审核数据"); }

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:query')")
    @GetMapping(value = "/{reviewId}")
    public AjaxResult getInfo(@PathVariable("reviewId") Long reviewId) { return success(samDegreeReviewService.selectSamDegreeReviewByReviewId(reviewId)); }

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:add')")
    @Log(title = "学位审核", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamDegreeReview samDegreeReview) { return toAjax(samDegreeReviewService.insertSamDegreeReview(samDegreeReview)); }

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:edit')")
    @Log(title = "学位审核", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamDegreeReview samDegreeReview) { return toAjax(samDegreeReviewService.updateSamDegreeReview(samDegreeReview)); }

    @PreAuthorize("@ss.hasPermi('sam:degreeReview:remove')")
    @Log(title = "学位审核", businessType = BusinessType.DELETE)
    @DeleteMapping("/{reviewIds}")
    public AjaxResult remove(@PathVariable Long[] reviewIds) { return toAjax(samDegreeReviewService.deleteSamDegreeReviewByReviewIds(reviewIds)); }
}
