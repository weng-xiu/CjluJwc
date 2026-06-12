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
import com.yu.brm.domain.BrmBuilding;
import com.yu.brm.service.IBrmBuildingService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/building")
public class BrmBuildingController extends BaseController
{
    @Autowired
    private IBrmBuildingService brmBuildingService;

    @PreAuthorize("@ss.hasPermi('brm:building:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmBuilding brmBuilding)
    {
        startPage();
        List<BrmBuilding> list = brmBuildingService.selectBrmBuildingList(brmBuilding);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:building:export')")
    @Log(title = "教学楼", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmBuilding brmBuilding)
    {
        List<BrmBuilding> list = brmBuildingService.selectBrmBuildingList(brmBuilding);
        ExcelUtil<BrmBuilding> util = new ExcelUtil<BrmBuilding>(BrmBuilding.class);
        util.exportExcel(response, list, "教学楼数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:building:query')")
    @GetMapping(value = "/{buildingId}")
    public AjaxResult getInfo(@PathVariable("buildingId") Long buildingId)
    {
        return success(brmBuildingService.selectBrmBuildingByBuildingId(buildingId));
    }

    @PreAuthorize("@ss.hasPermi('brm:building:add')")
    @Log(title = "教学楼", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmBuilding brmBuilding)
    {
        return toAjax(brmBuildingService.insertBrmBuilding(brmBuilding));
    }

    @PreAuthorize("@ss.hasPermi('brm:building:edit')")
    @Log(title = "教学楼", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmBuilding brmBuilding)
    {
        return toAjax(brmBuildingService.updateBrmBuilding(brmBuilding));
    }

    @PreAuthorize("@ss.hasPermi('brm:building:remove')")
    @Log(title = "教学楼", businessType = BusinessType.DELETE)
    @DeleteMapping("/{buildingIds}")
    public AjaxResult remove(@PathVariable Long[] buildingIds)
    {
        return toAjax(brmBuildingService.deleteBrmBuildingByBuildingIds(buildingIds));
    }
}
