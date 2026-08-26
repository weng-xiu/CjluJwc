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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.service.IAemEvaluationQuestionService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 评教问题Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/question")
public class AemEvaluationQuestionController extends BaseController
{
    @Autowired
    private IAemEvaluationQuestionService aemEvaluationQuestionService;

    @PreAuthorize("@ss.hasPermi('aem:question:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemEvaluationQuestion aemEvaluationQuestion)
    {
        startPage();
        List<AemEvaluationQuestion> list = aemEvaluationQuestionService.selectAemEvaluationQuestionList(aemEvaluationQuestion);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:question:export')")
    @Log(title = "评教问题", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemEvaluationQuestion aemEvaluationQuestion)
    {
        List<AemEvaluationQuestion> list = aemEvaluationQuestionService.selectAemEvaluationQuestionList(aemEvaluationQuestion);
        ExcelUtil<AemEvaluationQuestion> util = new ExcelUtil<AemEvaluationQuestion>(AemEvaluationQuestion.class);
        util.exportExcel(response, list, "评教问题数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:question:query')")
    @GetMapping(value = "/{questionId}")
    public AjaxResult getInfo(@PathVariable("questionId") Long questionId)
    {
        return success(aemEvaluationQuestionService.selectAemEvaluationQuestionByQuestionId(questionId));
    }

    @PreAuthorize("@ss.hasPermi('aem:question:add')")
    @Log(title = "评教问题", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemEvaluationQuestion aemEvaluationQuestion)
    {
        return toAjax(aemEvaluationQuestionService.insertAemEvaluationQuestion(aemEvaluationQuestion));
    }

    @PreAuthorize("@ss.hasPermi('aem:question:edit')")
    @Log(title = "评教问题", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemEvaluationQuestion aemEvaluationQuestion)
    {
        return toAjax(aemEvaluationQuestionService.updateAemEvaluationQuestion(aemEvaluationQuestion));
    }

    @PreAuthorize("@ss.hasPermi('aem:question:remove')")
    @Log(title = "评教问题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{questionIds}")
    public AjaxResult remove(@PathVariable Long[] questionIds)
    {
        return toAjax(aemEvaluationQuestionService.deleteAemEvaluationQuestionByQuestionIds(questionIds));
    }

    @PreAuthorize("@ss.hasPermi('aem:question:import')")
    @Log(title = "评教问题", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, @RequestParam(defaultValue = "false") boolean updateSupport) throws Exception
    {
        ExcelUtil<AemEvaluationQuestion> util = new ExcelUtil<AemEvaluationQuestion>(AemEvaluationQuestion.class);
        List<AemEvaluationQuestion> list = util.importExcel(file.getInputStream());
        String operator = getUsername();
        int rows = aemEvaluationQuestionService.importQuestion(list, operator);
        return success("导入成功，共 " + rows + " 条题目");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<AemEvaluationQuestion> util = new ExcelUtil<AemEvaluationQuestion>(AemEvaluationQuestion.class);
        util.importTemplateExcel(response, "评教题目数据");
    }
}
