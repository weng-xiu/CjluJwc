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
import com.yu.aem.service.IAemGradePredictionService;

/**
 * 成绩与学业预测Controller（F2-3 智能算法深化）。
 *
 * <p>只读分析接口：学生风险预测、课程难度画像、班级风险看板。
 * 预测结果全部由历史成绩实数算出，可解释、可回溯。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/aem/gradePrediction")
public class AemGradePredictionController extends BaseController
{
    @Autowired
    private IAemGradePredictionService gradePredictionService;

    /**
     * 学生学业风险预测（趋势拟合 + 分级 + 建议）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradePrediction:query')")
    @GetMapping("/studentRisk")
    public AjaxResult studentRisk(@RequestParam Long studentId)
    {
        return success(gradePredictionService.predictStudentRisk(studentId));
    }

    /**
     * 课程难度画像（semesterId 可选，不传为全量口径）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradePrediction:query')")
    @GetMapping("/courseDifficulty")
    public AjaxResult courseDifficulty(@RequestParam(required = false) Long semesterId)
    {
        List<Map<String, Object>> list = gradePredictionService.courseDifficultyProfile(semesterId);
        return success(list);
    }

    /**
     * 班级/学期学业风险看板（返回学生风险明细 + 分布统计；分布须基于全量数据，故不分页）
     */
    @PreAuthorize("@ss.hasPermi('aem:gradePrediction:query')")
    @GetMapping("/riskBoard")
    public AjaxResult riskBoard(@RequestParam Long semesterId,
                                @RequestParam(required = false) Long classId)
    {
        return success(gradePredictionService.riskBoard(semesterId, classId));
    }
}
