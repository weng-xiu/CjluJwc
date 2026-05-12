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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationResultService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 评教结果Controller
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/aem/evaluationResult")
public class AemEvaluationResultController extends BaseController
{
    @Autowired
    private IAemEvaluationResultService aemEvaluationResultService;

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemEvaluationResult aemEvaluationResult)
    {
        startPage();
        List<AemEvaluationResult> list = aemEvaluationResultService.selectAemEvaluationResultList(aemEvaluationResult);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:export')")
    @Log(title = "评教结果", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemEvaluationResult aemEvaluationResult)
    {
        List<AemEvaluationResult> list = aemEvaluationResultService.selectAemEvaluationResultList(aemEvaluationResult);
        ExcelUtil<AemEvaluationResult> util = new ExcelUtil<AemEvaluationResult>(AemEvaluationResult.class);
        util.exportExcel(response, list, "评教结果数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:query')")
    @GetMapping(value = "/{resultId}")
    public AjaxResult getInfo(@PathVariable("resultId") Long resultId)
    {
        return success(aemEvaluationResultService.selectAemEvaluationResultByResultId(resultId));
    }

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:add')")
    @Log(title = "评教结果", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AemEvaluationResult aemEvaluationResult)
    {
        return toAjax(aemEvaluationResultService.insertAemEvaluationResult(aemEvaluationResult));
    }

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:edit')")
    @Log(title = "评教结果", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AemEvaluationResult aemEvaluationResult)
    {
        return toAjax(aemEvaluationResultService.updateAemEvaluationResult(aemEvaluationResult));
    }

    @PreAuthorize("@ss.hasPermi('aem:evaluationResult:remove')")
    @Log(title = "评教结果", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resultIds}")
    public AjaxResult remove(@PathVariable Long[] resultIds)
    {
        return toAjax(aemEvaluationResultService.deleteAemEvaluationResultByResultIds(resultIds));
    }
}
