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
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;
import com.yu.aem.domain.AemGpaAlgorithmConfig;
import com.yu.aem.domain.AemGpaScoreMapping;
import com.yu.aem.service.IGpaAlgorithmConfigService;
import com.yu.aem.service.IAemGradeRecordService;

/**
 * GPA算法配置Controller
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/aem/gpaConfig")
public class GpaAlgorithmConfigController extends BaseController
{
    @Autowired
    private IGpaAlgorithmConfigService gpaAlgorithmConfigService;

    @Autowired
    private IAemGradeRecordService aemGradeRecordService;

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemGpaAlgorithmConfig aemGpaAlgorithmConfig)
    {
        startPage();
        List<AemGpaAlgorithmConfig> list = gpaAlgorithmConfigService.selectAlgorithmConfigList(aemGpaAlgorithmConfig);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:export')")
    @Log(title = "GPA算法配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AemGpaAlgorithmConfig aemGpaAlgorithmConfig)
    {
        List<AemGpaAlgorithmConfig> list = gpaAlgorithmConfigService.selectAlgorithmConfigList(aemGpaAlgorithmConfig);
        ExcelUtil<AemGpaAlgorithmConfig> util = new ExcelUtil<AemGpaAlgorithmConfig>(AemGpaAlgorithmConfig.class);
        util.exportExcel(response, list, "GPA算法配置数据");
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:query')")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable("configId") Long configId)
    {
        return success(gpaAlgorithmConfigService.selectAlgorithmConfigById(configId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:add')")
    @Log(title = "GPA算法配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AemGpaAlgorithmConfig aemGpaAlgorithmConfig)
    {
        return toAjax(gpaAlgorithmConfigService.insertAlgorithmConfig(aemGpaAlgorithmConfig));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:edit')")
    @Log(title = "GPA算法配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AemGpaAlgorithmConfig aemGpaAlgorithmConfig)
    {
        return toAjax(gpaAlgorithmConfigService.updateAlgorithmConfig(aemGpaAlgorithmConfig));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:remove')")
    @Log(title = "GPA算法配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        return toAjax(gpaAlgorithmConfigService.deleteAlgorithmConfigByIds(configIds));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:edit')")
    @Log(title = "GPA算法配置", businessType = BusinessType.UPDATE)
    @PostMapping("/setDefault/{configId}")
    public AjaxResult setDefault(@PathVariable("configId") Long configId)
    {
        return toAjax(gpaAlgorithmConfigService.setDefaultAlgorithm(configId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:query')")
    @GetMapping("/mappings/{configId}")
    public AjaxResult getMappings(@PathVariable("configId") Long configId)
    {
        return success(gpaAlgorithmConfigService.selectScoreMappings(configId));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:edit')")
    @Log(title = "GPA算法配置", businessType = BusinessType.UPDATE)
    @PostMapping("/mappings/{configId}")
    public AjaxResult saveMappings(@PathVariable("configId") Long configId, @RequestBody List<AemGpaScoreMapping> mappings)
    {
        return toAjax(gpaAlgorithmConfigService.saveScoreMappings(configId, mappings));
    }

    @PreAuthorize("@ss.hasPermi('aem:gpaConfig:edit')")
    @Log(title = "GPA算法配置", businessType = BusinessType.OTHER)
    @PostMapping("/recalculate")
    public AjaxResult recalculate(@RequestBody java.util.Map<String, Object> params)
    {
        Long semesterId = params.get("semesterId") != null ? Long.valueOf(params.get("semesterId").toString()) : null;
        String algorithmCode = params.get("algorithmCode") != null ? params.get("algorithmCode").toString() : null;
        aemGradeRecordService.batchRecalculateGpa(semesterId, algorithmCode);
        return success("重算完成");
    }
}
