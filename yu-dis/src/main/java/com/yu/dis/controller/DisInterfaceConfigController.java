package com.yu.dis.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.dis.domain.DisInterfaceConfig;
import com.yu.dis.service.IDisInterfaceConfigService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/dis/interface")
public class DisInterfaceConfigController extends BaseController
{
    @Autowired
    private IDisInterfaceConfigService disInterfaceConfigService;

    @PreAuthorize("@ss.hasPermi('dis:interface:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisInterfaceConfig disInterfaceConfig)
    {
        startPage();
        List<DisInterfaceConfig> list = disInterfaceConfigService.selectDisInterfaceConfigList(disInterfaceConfig);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:interface:export')")
    @Log(title = "接口配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisInterfaceConfig disInterfaceConfig)
    {
        List<DisInterfaceConfig> list = disInterfaceConfigService.selectDisInterfaceConfigList(disInterfaceConfig);
        ExcelUtil<DisInterfaceConfig> util = new ExcelUtil<DisInterfaceConfig>(DisInterfaceConfig.class);
        util.exportExcel(response, list, "接口配置数据");
    }

    @PreAuthorize("@ss.hasPermi('dis:interface:query')")
    @GetMapping(value = "/{interfaceId}")
    public AjaxResult getInfo(@PathVariable("interfaceId") Long interfaceId)
    {
        return success(disInterfaceConfigService.selectDisInterfaceConfigByInterfaceId(interfaceId));
    }

    @PreAuthorize("@ss.hasPermi('dis:interface:add')")
    @Log(title = "接口配置", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DisInterfaceConfig disInterfaceConfig)
    {
        return toAjax(disInterfaceConfigService.insertDisInterfaceConfig(disInterfaceConfig));
    }

    @PreAuthorize("@ss.hasPermi('dis:interface:edit')")
    @Log(title = "接口配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DisInterfaceConfig disInterfaceConfig)
    {
        return toAjax(disInterfaceConfigService.updateDisInterfaceConfig(disInterfaceConfig));
    }

    @PreAuthorize("@ss.hasPermi('dis:interface:remove')")
    @Log(title = "接口配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{interfaceIds}")
    public AjaxResult remove(@PathVariable Long[] interfaceIds)
    {
        return toAjax(disInterfaceConfigService.deleteDisInterfaceConfigByInterfaceIds(interfaceIds));
    }
}
