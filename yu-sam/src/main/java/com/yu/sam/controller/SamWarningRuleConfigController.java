package com.yu.sam.controller;

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
import com.yu.sam.domain.SamWarningRuleConfig;
import com.yu.sam.service.ISamWarningRuleConfigService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 预警规则配置Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/sam/warningRule")
public class SamWarningRuleConfigController extends BaseController
{
    @Autowired
    private ISamWarningRuleConfigService samWarningRuleConfigService;

    /**
     * 查询预警规则配置列表
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamWarningRuleConfig samWarningRuleConfig)
    {
        startPage();
        List<SamWarningRuleConfig> list = samWarningRuleConfigService.selectSamWarningRuleConfigList(samWarningRuleConfig);
        return getDataTable(list);
    }

    /**
     * 导出预警规则配置列表
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:export')")
    @Log(title = "预警规则配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamWarningRuleConfig samWarningRuleConfig)
    {
        List<SamWarningRuleConfig> list = samWarningRuleConfigService.selectSamWarningRuleConfigList(samWarningRuleConfig);
        ExcelUtil<SamWarningRuleConfig> util = new ExcelUtil<SamWarningRuleConfig>(SamWarningRuleConfig.class);
        util.exportExcel(response, list, "预警规则配置数据");
    }

    /**
     * 获取预警规则配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId)
    {
        return success(samWarningRuleConfigService.selectSamWarningRuleConfigById(ruleId));
    }

    /**
     * 新增预警规则配置
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:add')")
    @Log(title = "预警规则配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamWarningRuleConfig samWarningRuleConfig)
    {
        return toAjax(samWarningRuleConfigService.insertSamWarningRuleConfig(samWarningRuleConfig));
    }

    /**
     * 修改预警规则配置
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:edit')")
    @Log(title = "预警规则配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamWarningRuleConfig samWarningRuleConfig)
    {
        return toAjax(samWarningRuleConfigService.updateSamWarningRuleConfig(samWarningRuleConfig));
    }

    /**
     * 删除预警规则配置
     */
    @PreAuthorize("@ss.hasPermi('sam:warningRule:remove')")
    @Log(title = "预警规则配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds)
    {
        return toAjax(samWarningRuleConfigService.deleteSamWarningRuleConfigByIds(ruleIds));
    }
}
