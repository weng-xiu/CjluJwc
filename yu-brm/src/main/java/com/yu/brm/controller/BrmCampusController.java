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
import com.yu.brm.domain.BrmCampus;
import com.yu.brm.service.IBrmCampusService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/campus")
public class BrmCampusController extends BaseController
{
    @Autowired
    private IBrmCampusService brmCampusService;

    @PreAuthorize("@ss.hasPermi('brm:campus:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmCampus brmCampus)
    {
        startPage();
        List<BrmCampus> list = brmCampusService.selectBrmCampusList(brmCampus);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:campus:export')")
    @Log(title = "校区", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmCampus brmCampus)
    {
        List<BrmCampus> list = brmCampusService.selectBrmCampusList(brmCampus);
        ExcelUtil<BrmCampus> util = new ExcelUtil<BrmCampus>(BrmCampus.class);
        util.exportExcel(response, list, "校区数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:campus:query')")
    @GetMapping(value = "/{campusId}")
    public AjaxResult getInfo(@PathVariable("campusId") Long campusId)
    {
        return success(brmCampusService.selectBrmCampusByCampusId(campusId));
    }

    @PreAuthorize("@ss.hasPermi('brm:campus:add')")
    @Log(title = "校区", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmCampus brmCampus)
    {
        return toAjax(brmCampusService.insertBrmCampus(brmCampus));
    }

    @PreAuthorize("@ss.hasPermi('brm:campus:edit')")
    @Log(title = "校区", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmCampus brmCampus)
    {
        return toAjax(brmCampusService.updateBrmCampus(brmCampus));
    }

    @PreAuthorize("@ss.hasPermi('brm:campus:remove')")
    @Log(title = "校区", businessType = BusinessType.DELETE)
    @DeleteMapping("/{campusIds}")
    public AjaxResult remove(@PathVariable Long[] campusIds)
    {
        return toAjax(brmCampusService.deleteBrmCampusByCampusIds(campusIds));
    }
}
