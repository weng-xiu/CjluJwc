package com.yu.sam.controller;

import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;
import com.yu.sam.service.impl.AcademicWarningEngine;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/warning")
public class SamWarningController extends BaseController
{
    @Autowired
    private ISamWarningService samWarningService;

    @Autowired
    private AcademicWarningEngine academicWarningEngine;

    @PreAuthorize("@ss.hasPermi('sam:warning:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamWarning samWarning) { startPage(); List<SamWarning> list = samWarningService.selectSamWarningList(samWarning); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:warning:export')")
    @Log(title = "学籍预警", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamWarning samWarning) { List<SamWarning> list = samWarningService.selectSamWarningList(samWarning); ExcelUtil<SamWarning> util = new ExcelUtil<SamWarning>(SamWarning.class); util.exportExcel(response, list, "学籍预警数据"); }

    @PreAuthorize("@ss.hasPermi('sam:warning:query')")
    @GetMapping(value = "/{warningId}")
    public AjaxResult getInfo(@PathVariable("warningId") Long warningId) { return success(samWarningService.selectSamWarningByWarningId(warningId)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:add')")
    @Log(title = "学籍预警", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamWarning samWarning) { return toAjax(samWarningService.insertSamWarning(samWarning)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:edit')")
    @Log(title = "学籍预警", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamWarning samWarning) { return toAjax(samWarningService.updateSamWarning(samWarning)); }

    @PreAuthorize("@ss.hasPermi('sam:warning:remove')")
    @Log(title = "学籍预警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{warningIds}")
    public AjaxResult remove(@PathVariable Long[] warningIds) { return toAjax(samWarningService.deleteSamWarningByWarningIds(warningIds)); }

    /**
     * 学生查看自己的预警。
     * studentId 可不传，默认取当前登录用户ID。
     */
    @PreAuthorize("@ss.hasPermi('sam:warning:myList')")
    @GetMapping("/myWarnings")
    public TableDataInfo myWarnings(@RequestParam(required = false) Long studentId,
                                    @RequestParam(required = false) Long semesterId)
    {
        startPage();
        if (studentId == null)
        {
            studentId = getUserId();
        }
        SamWarning query = new SamWarning();
        query.setStudentId(studentId);
        query.setSemesterId(semesterId);
        List<SamWarning> list = samWarningService.selectSamWarningList(query);
        return getDataTable(list);
    }

    /**
     * 预警统计。
     * semesterId 不传时查询全部学期；学生角色只统计本人。
     */
    @PreAuthorize("@ss.hasPermi('sam:warning:statistics')")
    @GetMapping("/statistics")
    public AjaxResult statistics(@RequestParam(required = false) Long semesterId)
    {
        SamWarning query = new SamWarning();
        query.setSemesterId(semesterId);
        // 非管理员只统计本人数据
        if (!com.yu.common.utils.SecurityUtils.isAdmin(getUserId()))
        {
            query.setStudentId(getUserId());
        }
        List<SamWarning> list = samWarningService.selectSamWarningList(query);

        Map<String, Object> result = new HashMap<>();
        // 按类型统计
        long gpaCount = list.stream().filter(w -> "0".equals(w.getWarningType())).count();
        long creditCount = list.stream().filter(w -> "1".equals(w.getWarningType())).count();
        long attendanceCount = list.stream().filter(w -> "2".equals(w.getWarningType())).count();
        long comprehensiveCount = list.stream().filter(w -> "3".equals(w.getWarningType())).count();
        result.put("gpaCount", gpaCount);
        result.put("creditCount", creditCount);
        result.put("attendanceCount", attendanceCount);
        result.put("comprehensiveCount", comprehensiveCount);
        result.put("totalCount", list.size());

        // 按级别统计
        long normalCount = list.stream().filter(w -> "0".equals(w.getWarningLevel())).count();
        long seriousCount = list.stream().filter(w -> "1".equals(w.getWarningLevel())).count();
        long highRiskCount = list.stream().filter(w -> "2".equals(w.getWarningLevel())).count();
        result.put("normalCount", normalCount);
        result.put("seriousCount", seriousCount);
        result.put("highRiskCount", highRiskCount);

        // 未解除数量
        long unresolvedCount = list.stream().filter(w -> "0".equals(w.getIsResolved()) || w.getIsResolved() == null).count();
        result.put("unresolvedCount", unresolvedCount);

        return success(result);
    }

    /**
     * 手动触发批量生成预警
     */
    @PreAuthorize("@ss.hasPermi('sam:warning:generate')")
    @Log(title = "批量生成预警", businessType = BusinessType.INSERT)
    @PostMapping("/generateBatch")
    public AjaxResult generateBatch(@RequestParam Long semesterId)
    {
        Map<String, Object> result = academicWarningEngine.generateWarningsBatch(semesterId);
        return success(result);
    }
}
