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
    @GetMapping(value = "/{statId:\\d+}")
    public AjaxResult getInfo(@PathVariable("statId") Long statId)
    {
        return success(aemGradeStatisticsService.selectAemGradeStatisticsByStatId(statId));
    }

    /**
     * 按课程+学期触发聚合统计（SQL聚合，生成/刷新统计快照）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:edit')")
    @Log(title = "成绩统计分析", businessType = BusinessType.UPDATE)
    @PostMapping("/aggregate")
    public AjaxResult aggregate(@org.springframework.web.bind.annotation.RequestParam Long courseId,
                                @org.springframework.web.bind.annotation.RequestParam Long semesterId)
    {
        return success(aemGradeStatisticsService.aggregateByCourse(courseId, semesterId));
    }

    /**
     * 按学期批量聚合所有课程
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:edit')")
    @Log(title = "成绩统计分析", businessType = BusinessType.UPDATE)
    @PostMapping("/aggregateSemester")
    public AjaxResult aggregateSemester(@org.springframework.web.bind.annotation.RequestParam Long semesterId)
    {
        int count = aemGradeStatisticsService.aggregateBySemester(semesterId);
        return success("已聚合" + count + "门课程");
    }

    /**
     * 分数段分布（供图表）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:query')")
    @GetMapping("/distribution")
    public AjaxResult distribution(@org.springframework.web.bind.annotation.RequestParam Long courseId,
                                   @org.springframework.web.bind.annotation.RequestParam Long semesterId)
    {
        return success(aemGradeStatisticsService.scoreDistribution(courseId, semesterId));
    }

    /**
     * 学期成绩总览（仪表盘/汇总卡片）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:query')")
    @GetMapping("/overview")
    public AjaxResult overview(@org.springframework.web.bind.annotation.RequestParam Long semesterId)
    {
        return success(aemGradeStatisticsService.semesterOverview(semesterId));
    }

    /**
     * 课程成绩排名（分页）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradeStatistics:query')")
    @GetMapping("/ranking")
    public TableDataInfo ranking(@org.springframework.web.bind.annotation.RequestParam Long courseId,
                                 @org.springframework.web.bind.annotation.RequestParam Long semesterId)
    {
        startPage();
        List<java.util.Map<String, Object>> list = aemGradeStatisticsService.courseRanking(courseId, semesterId);
        return getDataTable(list);
    }
}
