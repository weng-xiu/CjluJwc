package com.yu.aem.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemGradeStatistics;
import com.yu.aem.service.IAemGradeStatisticsService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 成绩统计分析Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/gradeStatistics")
public class AemGradeStatisticsController extends BaseController
{
    @Autowired
    private IAemGradeStatisticsService aemGradeStatisticsService;

    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGradeStatistics aemGradeStatistics)
    {
        startPage();
        List<AemGradeStatistics> list = aemGradeStatisticsService.selectAemGradeStatisticsList(aemGradeStatistics);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:export')")
    @Log(title = "成绩统计分析", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemGradeStatistics aemGradeStatistics)
    {
        List<AemGradeStatistics> list = aemGradeStatisticsService.selectAemGradeStatisticsList(aemGradeStatistics);
        ExcelUtil<AemGradeStatistics> util = new ExcelUtil<AemGradeStatistics>(AemGradeStatistics.class);
        util.exportExcel(response, list, "成绩统计分析数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:query')")
    @GetMapping(value = "/{statId}")
    public AjaxResult getInfo(@PathVariable("statId") Long statId)
    {
        return success(aemGradeStatisticsService.selectAemGradeStatisticsByStatId(statId));
    }
}
