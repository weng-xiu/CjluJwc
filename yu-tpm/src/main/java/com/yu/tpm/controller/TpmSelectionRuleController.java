package com.yu.tpm.controller;

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
import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.service.ITpmSelectionRuleService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 选课规则Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/rule")
public class TpmSelectionRuleController extends BaseController
{
    @Autowired
    private ITpmSelectionRuleService tpmSelectionRuleService;

    @PreAuthorize("@ss.hasPermi('tpm:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmSelectionRule tpmSelectionRule)
    {
        startPage();
        List<TpmSelectionRule> list = tpmSelectionRuleService.selectTpmSelectionRuleList(tpmSelectionRule);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:rule:export')")
    @Log(title = "选课规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmSelectionRule tpmSelectionRule)
    {
        List<TpmSelectionRule> list = tpmSelectionRuleService.selectTpmSelectionRuleList(tpmSelectionRule);
        ExcelUtil<TpmSelectionRule> util = new ExcelUtil<TpmSelectionRule>(TpmSelectionRule.class);
        util.exportExcel(response, list, "选课规则数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:rule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return success(tpmSelectionRuleService.selectTpmSelectionRuleByRuleId(ruleId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:rule:add')")
    @Log(title = "选课规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TpmSelectionRule tpmSelectionRule)
    {
        return toAjax(tpmSelectionRuleService.insertTpmSelectionRule(tpmSelectionRule));
    }

    @PreAuthorize("@ss.hasPermi('tpm:rule:edit')")
    @Log(title = "选课规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TpmSelectionRule tpmSelectionRule)
    {
        return toAjax(tpmSelectionRuleService.updateTpmSelectionRule(tpmSelectionRule));
    }

    @PreAuthorize("@ss.hasPermi('tpm:rule:remove')")
    @Log(title = "选课规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(tpmSelectionRuleService.deleteTpmSelectionRuleByRuleIds(ruleIds));
    }
}
