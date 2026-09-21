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
import com.yu.sam.domain.SamDegreeConfig;
import com.yu.sam.service.ISamDegreeConfigService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 学位授予条件配置Controller（S3）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@RestController
@RequestMapping("/sam/degreeConfig")
public class SamDegreeConfigController extends BaseController
{
    @Autowired
    private ISamDegreeConfigService samDegreeConfigService;

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamDegreeConfig samDegreeConfig) { startPage(); List<SamDegreeConfig> list = samDegreeConfigService.selectSamDegreeConfigList(samDegreeConfig); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:export')")
    @Log(title = "学位授予条件配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamDegreeConfig samDegreeConfig) { List<SamDegreeConfig> list = samDegreeConfigService.selectSamDegreeConfigList(samDegreeConfig); ExcelUtil<SamDegreeConfig> util = new ExcelUtil<SamDegreeConfig>(SamDegreeConfig.class); util.exportExcel(response, list, "学位授予条件配置数据"); }

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:query')")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable("configId") Long configId) { return success(samDegreeConfigService.selectSamDegreeConfigByConfigId(configId)); }

    /** 查询当前生效配置（学位审核实际使用的口径） */
    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:query')")
    @GetMapping("/effective")
    public AjaxResult effective() { return success(samDegreeConfigService.resolveEffective()); }

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:add')")
    @Log(title = "学位授予条件配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SamDegreeConfig samDegreeConfig) { samDegreeConfig.setCreateBy(getUsername()); return toAjax(samDegreeConfigService.insertSamDegreeConfig(samDegreeConfig)); }

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:edit')")
    @Log(title = "学位授予条件配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamDegreeConfig samDegreeConfig) { samDegreeConfig.setUpdateBy(getUsername()); return toAjax(samDegreeConfigService.updateSamDegreeConfig(samDegreeConfig)); }

    /** 设为默认配置 */
    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:edit')")
    @Log(title = "学位授予条件配置", businessType = BusinessType.UPDATE)
    @PutMapping("/default/{configId}")
    public AjaxResult setDefault(@PathVariable("configId") Long configId) { return toAjax(samDegreeConfigService.setDefault(configId)); }

    @PreAuthorize("@ss.hasPermi('sam:degreeConfig:remove')")
    @Log(title = "学位授予条件配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds) { return toAjax(samDegreeConfigService.deleteSamDegreeConfigByConfigIds(configIds)); }
}
