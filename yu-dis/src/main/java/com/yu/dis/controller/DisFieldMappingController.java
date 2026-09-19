package com.yu.dis.controller;

import java.util.List;
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
import com.yu.dis.domain.DisFieldMapping;
import com.yu.dis.service.IDisFieldMappingService;
import com.yu.common.core.page.TableDataInfo;

/**
 * 数据同步字段映射Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/dis/fieldMapping")
public class DisFieldMappingController extends BaseController
{
    @Autowired
    private IDisFieldMappingService disFieldMappingService;

    @PreAuthorize("@ss.hasPermi('dis:fieldMapping:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisFieldMapping disFieldMapping)
    {
        startPage();
        List<DisFieldMapping> list = disFieldMappingService.selectDisFieldMappingList(disFieldMapping);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:fieldMapping:query')")
    @GetMapping(value = "/{mappingId}")
    public AjaxResult getInfo(@PathVariable("mappingId") Long mappingId)
    {
        return success(disFieldMappingService.selectDisFieldMappingByMappingId(mappingId));
    }

    @PreAuthorize("@ss.hasPermi('dis:fieldMapping:add')")
    @Log(title = "字段映射", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DisFieldMapping disFieldMapping)
    {
        return toAjax(disFieldMappingService.insertDisFieldMapping(disFieldMapping));
    }

    @PreAuthorize("@ss.hasPermi('dis:fieldMapping:edit')")
    @Log(title = "字段映射", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DisFieldMapping disFieldMapping)
    {
        return toAjax(disFieldMappingService.updateDisFieldMapping(disFieldMapping));
    }

    @PreAuthorize("@ss.hasPermi('dis:fieldMapping:remove')")
    @Log(title = "字段映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{mappingIds}")
    public AjaxResult remove(@PathVariable Long[] mappingIds)
    {
        return toAjax(disFieldMappingService.deleteDisFieldMappingByMappingIds(mappingIds));
    }
}
