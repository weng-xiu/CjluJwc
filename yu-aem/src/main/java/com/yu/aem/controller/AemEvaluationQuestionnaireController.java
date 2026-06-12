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
import com.yu.aem.domain.AemEvaluationQuestionnaire;
import com.yu.aem.service.IAemEvaluationQuestionnaireService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 评教问卷配置Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/questionnaire")
public class AemEvaluationQuestionnaireController extends BaseController
{
    @Autowired
    private IAemEvaluationQuestionnaireService aemEvaluationQuestionnaireService;

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        startPage();
        List<AemEvaluationQuestionnaire> list = aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(aemEvaluationQuestionnaire);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:export')")
    @Log(title = "评教问卷配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        List<AemEvaluationQuestionnaire> list = aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireList(aemEvaluationQuestionnaire);
        ExcelUtil<AemEvaluationQuestionnaire> util = new ExcelUtil<AemEvaluationQuestionnaire>(AemEvaluationQuestionnaire.class);
        util.exportExcel(response, list, "评教问卷配置数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:query')")
    @GetMapping(value = "/{questionnaireId}")
    public AjaxResult getInfo(@PathVariable("questionnaireId") Long questionnaireId)
    {
        return success(aemEvaluationQuestionnaireService.selectAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId));
    }

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:add')")
    @Log(title = "评教问卷配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        return toAjax(aemEvaluationQuestionnaireService.insertAemEvaluationQuestionnaire(aemEvaluationQuestionnaire));
    }

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:edit')")
    @Log(title = "评教问卷配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        return toAjax(aemEvaluationQuestionnaireService.updateAemEvaluationQuestionnaire(aemEvaluationQuestionnaire));
    }

    @PreAuthorize("@ss.hasPermi('aem:questionnaire:remove')")
    @Log(title = "评教问卷配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{questionnaireIds}")
    public AjaxResult remove(@PathVariable Long[] questionnaireIds)
    {
        return toAjax(aemEvaluationQuestionnaireService.deleteAemEvaluationQuestionnaireByQuestionnaireIds(questionnaireIds));
    }
}
