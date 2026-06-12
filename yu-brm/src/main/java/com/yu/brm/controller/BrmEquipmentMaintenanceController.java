package com.yu.brm.controller;

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
import com.yu.brm.domain.BrmEquipmentMaintenance;
import com.yu.brm.service.IBrmEquipmentMaintenanceService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/maintenance")
public class BrmEquipmentMaintenanceController extends BaseController
{
    @Autowired
    private IBrmEquipmentMaintenanceService brmEquipmentMaintenanceService;

    @PreAuthorize("@ss.hasPermi('brm:maintenance:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        startPage();
        List<BrmEquipmentMaintenance> list = brmEquipmentMaintenanceService.selectBrmEquipmentMaintenanceList(brmEquipmentMaintenance);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:maintenance:export')")
    @Log(title = "设备维护记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        List<BrmEquipmentMaintenance> list = brmEquipmentMaintenanceService.selectBrmEquipmentMaintenanceList(brmEquipmentMaintenance);
        ExcelUtil<BrmEquipmentMaintenance> util = new ExcelUtil<BrmEquipmentMaintenance>(BrmEquipmentMaintenance.class);
        util.exportExcel(response, list, "设备维护记录数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:maintenance:query')")
    @GetMapping(value = "/{maintenanceId}")
    public AjaxResult getInfo(@PathVariable("maintenanceId") Long maintenanceId)
    {
        return success(brmEquipmentMaintenanceService.selectBrmEquipmentMaintenanceByMaintenanceId(maintenanceId));
    }

    @PreAuthorize("@ss.hasPermi('brm:maintenance:add')")
    @Log(title = "设备维护记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        return toAjax(brmEquipmentMaintenanceService.insertBrmEquipmentMaintenance(brmEquipmentMaintenance));
    }

    @PreAuthorize("@ss.hasPermi('brm:maintenance:edit')")
    @Log(title = "设备维护记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmEquipmentMaintenance brmEquipmentMaintenance)
    {
        return toAjax(brmEquipmentMaintenanceService.updateBrmEquipmentMaintenance(brmEquipmentMaintenance));
    }

    @PreAuthorize("@ss.hasPermi('brm:maintenance:remove')")
    @Log(title = "设备维护记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{maintenanceIds}")
    public AjaxResult remove(@PathVariable Long[] maintenanceIds)
    {
        return toAjax(brmEquipmentMaintenanceService.deleteBrmEquipmentMaintenanceByMaintenanceIds(maintenanceIds));
    }
}
