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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.dis.domain.DisExternalSystem;
import com.yu.dis.service.IDisExternalSystemService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/dis/system")
public class DisExternalSystemController extends BaseController
{
    @Autowired
    private IDisExternalSystemService disExternalSystemService;

    @PreAuthorize("@ss.hasPermi('dis:system:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisExternalSystem disExternalSystem)
    {
        startPage();
        List<DisExternalSystem> list = disExternalSystemService.selectDisExternalSystemList(disExternalSystem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:system:export')")
    @Log(title = "外部系统", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisExternalSystem disExternalSystem)
    {
        List<DisExternalSystem> list = disExternalSystemService.selectDisExternalSystemList(disExternalSystem);
        ExcelUtil<DisExternalSystem> util = new ExcelUtil<DisExternalSystem>(DisExternalSystem.class);
        util.exportExcel(response, list, "外部系统数据");
    }

    @PreAuthorize("@ss.hasPermi('dis:system:query')")
    @GetMapping(value = "/{systemId}")
    public AjaxResult getInfo(@PathVariable("systemId") Long systemId)
    {
        return success(disExternalSystemService.selectDisExternalSystemBySystemId(systemId));
    }

    @PreAuthorize("@ss.hasPermi('dis:system:add')")
    @Log(title = "外部系统", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DisExternalSystem disExternalSystem)
    {
        return toAjax(disExternalSystemService.insertDisExternalSystem(disExternalSystem));
    }

    @PreAuthorize("@ss.hasPermi('dis:system:edit')")
    @Log(title = "外部系统", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DisExternalSystem disExternalSystem)
    {
        return toAjax(disExternalSystemService.updateDisExternalSystem(disExternalSystem));
    }

    @PreAuthorize("@ss.hasPermi('dis:system:remove')")
    @Log(title = "外部系统", businessType = BusinessType.DELETE)
    @DeleteMapping("/{systemIds}")
    public AjaxResult remove(@PathVariable Long[] systemIds)
    {
        return toAjax(disExternalSystemService.deleteDisExternalSystemBySystemIds(systemIds));
    }
}
