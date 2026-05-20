package com.yu.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.domain.AemGradeStatistics;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IAemGradeStatisticsService;

/**
 * 成绩查询与录入门户Controller
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/grade")
public class PortalGradeController extends BaseController
{
    @Autowired
    private IAemGradeRecordService aemGradeRecordService;

    @Autowired
    private IAemGradeStatisticsService aemGradeStatisticsService;

    /** 学生端：成绩查询 */
    @PreAuthorize("@ss.hasPermi('portal:grade:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGradeRecord aemGradeRecord)
    {
        startPage();
        List<AemGradeRecord> list = aemGradeRecordService.selectAemGradeRecordList(aemGradeRecord);
        return getDataTable(list);
    }

    /** 教师端：成绩录入查询（教学班成绩列表） */
    @PreAuthorize("@ss.hasPermi('portal:gradeEntry:list')")
    @GetMapping("/entryList")
    public TableDataInfo entryList(AemGradeRecord aemGradeRecord)
    {
        startPage();
        List<AemGradeRecord> list = aemGradeRecordService.selectAemGradeRecordList(aemGradeRecord);
        return getDataTable(list);
    }

    /** 教师端：录入成绩 */
    @PreAuthorize("@ss.hasPermi('portal:gradeEntry:add')")
    @Log(title = "成绩录入", businessType = BusinessType.INSERT)
    @PostMapping("/entry")
    public AjaxResult entry(@RequestBody AemGradeRecord aemGradeRecord)
    {
        return toAjax(aemGradeRecordService.insertAemGradeRecord(aemGradeRecord));
    }

    /** 教师端：修改成绩 */
    @PreAuthorize("@ss.hasPermi('portal:gradeEntry:edit')")
    @Log(title = "成绩修改", businessType = BusinessType.UPDATE)
    @PutMapping("/entry")
    public AjaxResult editEntry(@RequestBody AemGradeRecord aemGradeRecord)
    {
        return toAjax(aemGradeRecordService.updateAemGradeRecord(aemGradeRecord));
    }

    /** 教师端：暂存/提交成绩（含平时成绩和期末成绩） */
    @PreAuthorize("@ss.hasPermi('portal:gradeEntry:add')")
    @Log(title = "成绩提交", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody Map<String, Object> params)
    {
        AemGradeRecord record = new AemGradeRecord();
        Object recordId = params.get("recordId");
        if (recordId != null) {
            record.setGradeId(Long.valueOf(recordId.toString()));
        }
        Object regularScore = params.get("regularScore");
        if (regularScore != null) {
            record.setRegularScore(Double.valueOf(regularScore.toString()));
        }
        Object finalScore = params.get("finalScore");
        if (finalScore != null) {
            record.setExamScore(Double.valueOf(finalScore.toString()));
        }
        return toAjax(aemGradeRecordService.updateAemGradeRecord(record));
    }

    /** 学生端：成绩统计分析 */
    @PreAuthorize("@ss.hasPermi('portal:grade:statistics')")
    @GetMapping("/statistics")
    public TableDataInfo statistics()
    {
        startPage();
        List<AemGradeStatistics> list = aemGradeStatisticsService.selectAemGradeStatisticsList(new AemGradeStatistics());
        return getDataTable(list);
    }
}
