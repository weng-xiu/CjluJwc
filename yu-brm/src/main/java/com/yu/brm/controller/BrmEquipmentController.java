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
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmEquipment;
import com.yu.brm.service.IBrmEquipmentService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/equip")
public class BrmEquipmentController extends BaseController
{
    @Autowired
    private IBrmEquipmentService brmEquipmentService;

    @PreAuthorize("@ss.hasPermi('brm:equip:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmEquipment brmEquipment)
    {
        startPage();
        List<BrmEquipment> list = brmEquipmentService.selectBrmEquipmentList(brmEquipment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:equip:export')")
    @Log(title = "多媒体设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmEquipment brmEquipment)
    {
        List<BrmEquipment> list = brmEquipmentService.selectBrmEquipmentList(brmEquipment);
        ExcelUtil<BrmEquipment> util = new ExcelUtil<BrmEquipment>(BrmEquipment.class);
        util.exportExcel(response, list, "多媒体设备数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:equip:query')")
    @GetMapping(value = "/{equipId}")
    public AjaxResult getInfo(@PathVariable("equipId") Long equipId)
    {
        return success(brmEquipmentService.selectBrmEquipmentByEquipId(equipId));
    }

    @PreAuthorize("@ss.hasPermi('brm:equip:add')")
    @Log(title = "多媒体设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BrmEquipment brmEquipment)
    {
        return toAjax(brmEquipmentService.insertBrmEquipment(brmEquipment));
    }

    @PreAuthorize("@ss.hasPermi('brm:equip:edit')")
    @Log(title = "多媒体设备", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BrmEquipment brmEquipment)
    {
        return toAjax(brmEquipmentService.updateBrmEquipment(brmEquipment));
    }

    @PreAuthorize("@ss.hasPermi('brm:equip:remove')")
    @Log(title = "多媒体设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{equipIds}")
    public AjaxResult remove(@PathVariable Long[] equipIds)
    {
        return toAjax(brmEquipmentService.deleteBrmEquipmentByEquipIds(equipIds));
    }
}
