package com.yu.aem.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.aem.service.IAemEvaluationStatService;

/**
 * 评教统计Controller（A6：多维统计与反馈分析）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@RestController
@RequestMapping("/aem/evaluationStat")
public class AemEvaluationStatController extends BaseController
{
    @Autowired
    private IAemEvaluationStatService aemEvaluationStatService;

    /** 总览：核心指标 + 分数段分布 */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) Long questionnaireId,
                               @RequestParam(required = false) Long semesterId)
    {
        return success(aemEvaluationStatService.overview(questionnaireId, semesterId));
    }

    /** 按课程聚合 */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/byCourse")
    public AjaxResult byCourse(@RequestParam(required = false) Long questionnaireId,
                               @RequestParam(required = false) Long semesterId)
    {
        List<Map<String, Object>> list = aemEvaluationStatService.byCourse(questionnaireId, semesterId);
        return success(list);
    }

    /** 按教师聚合（含排名） */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/byTeacher")
    public AjaxResult byTeacher(@RequestParam(required = false) Long questionnaireId,
                                @RequestParam(required = false) Long semesterId)
    {
        List<Map<String, Object>> list = aemEvaluationStatService.byTeacher(questionnaireId, semesterId);
        return success(list);
    }

    /** 按班级聚合 */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/byClass")
    public AjaxResult byClass(@RequestParam(required = false) Long questionnaireId,
                              @RequestParam(required = false) Long semesterId,
                              @RequestParam(required = false) Long courseId)
    {
        List<Map<String, Object>> list = aemEvaluationStatService.byClass(questionnaireId, semesterId, courseId);
        return success(list);
    }

    /** 月度趋势 */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(required = false) Long questionnaireId)
    {
        return success(aemEvaluationStatService.trend(questionnaireId));
    }

    /** 评语词频分析 */
    @PreAuthorize("@ss.hasPermi('aem:evaluationStat:list')")
    @GetMapping("/commentAnalysis")
    public AjaxResult commentAnalysis(@RequestParam(required = false) Long teacherId,
                                      @RequestParam(required = false) Long courseId,
                                      @RequestParam(required = false) Long questionnaireId)
    {
        return success(aemEvaluationStatService.commentAnalysis(teacherId, courseId, questionnaireId));
    }
}
