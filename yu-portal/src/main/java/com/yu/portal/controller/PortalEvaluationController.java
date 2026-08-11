package com.yu.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.domain.AemEvaluationQuestionnaire;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationQuestionService;
import com.yu.aem.service.IAemEvaluationQuestionnaireService;
import com.yu.aem.service.IAemEvaluationResultService;

/**
 * 评教门户Controller（学生评教入口 + 教师评教结果查询）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/evaluation")
public class PortalEvaluationController extends BaseController
{
    @Autowired
    private IAemEvaluationQuestionnaireService aemEvaluationQuestionnaireService;

    @Autowired
    private IAemEvaluationQuestionService aemEvaluationQuestionService;

    @Autowired
    private IAemEvaluationResultService aemEvaluationResultService;

    /** 学生端：待评教问卷列表 */
    @PreAuthorize("@ss.hasPermi('portal:evaluation:list')")
    @GetMapping("/questionnaireList")
    public TableDataInfo questionnaireList(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        startPage();
        List<AemEvaluationQuestionnaire> list = aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(aemEvaluationQuestionnaire);
        return getDataTable(list);
    }

    /** 学生端：提交评教 */
    @PreAuthorize("@ss.hasPermi('portal:evaluation:submit')")
    @Log(title = "评教提交", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody AemEvaluationResult aemEvaluationResult)
    {
        return toAjax(aemEvaluationResultService.insertAemEvaluationResult(aemEvaluationResult));
    }

    /** 学生端：获取评教状态 */
    @PreAuthorize("@ss.hasPermi('portal:evaluation:list')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        AemEvaluationQuestionnaire query = new AemEvaluationQuestionnaire();
        query.setEvalStatus("1");
        List<AemEvaluationQuestionnaire> activeList = aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(query);
        Map<String, Object> result = new HashMap<>();
        if (activeList != null && !activeList.isEmpty()) {
            result.put("completed", false);
            result.put("message", "当前有 " + activeList.size() + " 份评教问卷待完成");
        } else {
            result.put("completed", true);
            result.put("message", "评教已完成，感谢您的参与！");
        }
        return success(result);
    }

    /** 学生端：获取问卷题目列表 */
    @PreAuthorize("@ss.hasPermi('portal:evaluation:list')")
    @GetMapping("/questions/{questionnaireId}")
    public AjaxResult questions(@PathVariable Long questionnaireId)
    {
        AemEvaluationQuestion query = new AemEvaluationQuestion();
        query.setQuestionnaireId(questionnaireId);
        query.setStatus("0");
        List<AemEvaluationQuestion> list = aemEvaluationQuestionService.selectAemEvaluationQuestionList(query);
        List<Map<String, Object>> resultList = list.stream().map(q -> {
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getQuestionId());
            m.put("questionType", q.getQuestionType());
            m.put("questionTitle", q.getQuestionContent());
            m.put("maxScore", q.getMaxScore());
            m.put("options", q.getOptionsJson());
            m.put("sortOrder", q.getSortOrder());
            return m;
        }).collect(Collectors.toList());
        return success(resultList);
    }

    /** 教师端：评教结果查询 */
    @PreAuthorize("@ss.hasPermi('portal:evalResult:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/resultList")
    public TableDataInfo resultList(AemEvaluationResult aemEvaluationResult)
    {
        startPage();
        List<AemEvaluationResult> list = aemEvaluationResultService.selectAemEvaluationResultList(aemEvaluationResult);
        return getDataTable(list);
    }

    /** 教师端：评教结果汇总（前端调用） */
    @PreAuthorize("@ss.hasPermi('portal:evalResult:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/teacherResults")
    public TableDataInfo teacherResults()
    {
        startPage();
        List<AemEvaluationResult> list = aemEvaluationResultService.selectAemEvaluationResultList(new AemEvaluationResult());
        List<Map<String, Object>> resultList = list.stream().collect(Collectors.groupingBy(
            AemEvaluationResult::getCourseId,
            Collectors.collectingAndThen(Collectors.toList(), results -> {
                Map<String, Object> m = new HashMap<>();
                AemEvaluationResult first = results.get(0);
                m.put("courseId", first.getCourseId());
                m.put("courseName", "课程" + first.getCourseId());
                m.put("semesterName", "本学期");
                m.put("participantCount", results.size());
                m.put("avgScore", results.stream().mapToDouble(r -> r.getTotalScore() != null ? r.getTotalScore() : 0.0).average().orElse(0.0));
                return m;
            })
        )).values().stream().collect(Collectors.toList());
        return getDataTable(resultList);
    }

    /** 教师端：获取某门课程的真实评语列表（非空评语） */
    @PreAuthorize("@ss.hasPermi('portal:evalResult:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/comments/{courseId}")
    public AjaxResult courseComments(@PathVariable Long courseId)
    {
        AemEvaluationResult query = new AemEvaluationResult();
        query.setCourseId(courseId);
        List<AemEvaluationResult> list = aemEvaluationResultService.selectAemEvaluationResultList(query);
        List<String> comments = list.stream()
                .map(AemEvaluationResult::getComment)
                .filter(c -> c != null && !c.trim().isEmpty())
                .collect(Collectors.toList());
        return success(comments);
    }
}
