package com.yu.web.controller.system;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.system.domain.SysAiEvalCase;
import com.yu.system.domain.SysAiEvalResult;
import com.yu.system.domain.SysAiEvalRun;
import com.yu.system.service.ISysAiEvalService;
import com.yu.system.service.ISysConfigService;

/**
 * 智能体发布前评测 信息操作处理（K4 AI 治理 · 评测集存档 + 发布门禁）
 *
 * 把当前单一 RAG 教务问答助手视为「智能体」：管理员维护 golden 评测用例，发布/更新前跑分留痕，
 * 通过率低于参数阈值即拦截发布。批次与逐条结果为审计数据，只读不删。
 *
 * @author yu
 * @date 2026-10-12
 */
@RestController
@RequestMapping("/system/aiEval")
public class SysAiEvalController extends BaseController
{
    /** 发布门禁总开关参数；缺省视为开启 */
    private static final String CFG_PUBLISH_ENABLED = "ai.eval.publish.enabled";

    @Autowired
    private ISysAiEvalService evalService;

    @Autowired
    private ISysConfigService configService;

    // ===================== 评测用例（golden set） =====================

    /** 查询评测用例列表 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:list')")
    @GetMapping("/case/list")
    public TableDataInfo caseList(SysAiEvalCase evalCase)
    {
        startPage();
        List<SysAiEvalCase> list = evalService.selectEvalCaseList(evalCase);
        return getDataTable(list);
    }

    /** 导出评测用例 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:export')")
    @Log(title = "AI评测用例", businessType = BusinessType.EXPORT)
    @PostMapping("/case/export")
    public void caseExport(HttpServletResponse response, SysAiEvalCase evalCase)
    {
        List<SysAiEvalCase> list = evalService.selectEvalCaseList(evalCase);
        ExcelUtil<SysAiEvalCase> util = new ExcelUtil<SysAiEvalCase>(SysAiEvalCase.class);
        util.exportExcel(response, list, "智能体评测用例");
    }

    /** 获取评测用例详情 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:query')")
    @GetMapping(value = "/case/{caseId}")
    public AjaxResult getCase(@PathVariable Long caseId)
    {
        return success(evalService.selectEvalCaseById(caseId));
    }

    /** 新增评测用例 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:add')")
    @Log(title = "AI评测用例", businessType = BusinessType.INSERT)
    @PostMapping("/case")
    public AjaxResult addCase(@Validated @RequestBody SysAiEvalCase evalCase)
    {
        evalCase.setCreateBy(getUsername());
        return toAjax(evalService.insertEvalCase(evalCase));
    }

    /** 修改评测用例 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:edit')")
    @Log(title = "AI评测用例", businessType = BusinessType.UPDATE)
    @PutMapping("/case")
    public AjaxResult editCase(@Validated @RequestBody SysAiEvalCase evalCase)
    {
        evalCase.setUpdateBy(getUsername());
        return toAjax(evalService.updateEvalCase(evalCase));
    }

    /** 删除评测用例 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:remove')")
    @Log(title = "AI评测用例", businessType = BusinessType.DELETE)
    @DeleteMapping("/case/{caseIds}")
    public AjaxResult removeCase(@PathVariable Long[] caseIds)
    {
        return toAjax(evalService.deleteEvalCaseByIds(caseIds));
    }

    // ===================== 评测批次 & 发布门禁 =====================

    /** 手动运行评测（以全部启用用例跑分并留痕） */
    @PreAuthorize("@ss.hasPermi('system:aiEval:run')")
    @Log(title = "AI发布前评测", businessType = BusinessType.OTHER)
    @PostMapping("/run")
    public AjaxResult run()
    {
        SysAiEvalRun run = evalService.evaluate(SysAiEvalRun.TRIGGER_MANUAL, getUsername());
        return success(run);
    }

    /** 发布门禁：以评测通过率决定是否放行发布/更新；低于阈值即拦截 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:publish')")
    @Log(title = "AI发布门禁", businessType = BusinessType.OTHER)
    @PostMapping("/publishCheck")
    public AjaxResult publishCheck()
    {
        String enabled = configService.selectConfigByKey(CFG_PUBLISH_ENABLED);
        if ("false".equalsIgnoreCase(StringUtils.trim(enabled)))
        {
            // 门禁开关显式关闭：登记一次手动评测留痕后直接放行，便于灰度期不误伤发布
            SysAiEvalRun run = evalService.evaluate(SysAiEvalRun.TRIGGER_PUBLISH, getUsername());
            return AjaxResult.success("发布门禁已关闭，本次仅评测留痕", run);
        }
        SysAiEvalRun run = evalService.evaluate(SysAiEvalRun.TRIGGER_PUBLISH, getUsername());
        if (SysAiEvalRun.RESULT_BLOCK.equals(run.getResult()))
        {
            AjaxResult ajax = AjaxResult.error("发布被拦截：评测通过率 " + nvl(run.getPassRate()) + "% 低于阈值 "
                    + nvl(run.getPassThreshold()) + "%，请优化知识库/提示词后重试");
            ajax.put("run", run);
            return ajax;
        }
        return AjaxResult.success("评测通过，允许发布", run);
    }

    /** 评测批次列表 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:list')")
    @GetMapping("/run/list")
    public TableDataInfo runList(SysAiEvalRun run)
    {
        startPage();
        List<SysAiEvalRun> list = evalService.selectEvalRunList(run);
        return getDataTable(list);
    }

    /** 评测批次详情 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:query')")
    @GetMapping(value = "/run/{runId}")
    public AjaxResult getRun(@PathVariable Long runId)
    {
        return success(evalService.selectEvalRunById(runId));
    }

    /** 评测批次逐条结果 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:query')")
    @GetMapping("/run/{runId}/results")
    public AjaxResult runResults(@PathVariable Long runId)
    {
        return success(evalService.selectResultsByRunId(runId));
    }

    /** 导出评测批次逐条结果 */
    @PreAuthorize("@ss.hasPermi('system:aiEval:export')")
    @Log(title = "AI评测结果", businessType = BusinessType.EXPORT)
    @PostMapping("/run/{runId}/export")
    public void exportResults(HttpServletResponse response, @PathVariable Long runId)
    {
        List<SysAiEvalResult> list = evalService.selectResultsByRunId(runId);
        ExcelUtil<SysAiEvalResult> util = new ExcelUtil<SysAiEvalResult>(SysAiEvalResult.class);
        util.exportExcel(response, list, "智能体评测逐条结果");
    }

    private String nvl(Double d)
    {
        return d == null ? "0" : String.valueOf(d);
    }
}
