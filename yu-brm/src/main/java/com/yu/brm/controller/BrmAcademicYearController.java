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
import com.yu.brm.domain.BrmAcademicYear;
import com.yu.brm.service.IBrmAcademicYearService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/year")
public class BrmAcademicYearController extends BaseController
{
    @Autowired
    private IBrmAcademicYearService brmAcademicYearService;

    @PreAuthorize("@ss.hasPermi('brm:year:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmAcademicYear brmAcademicYear)
    {
        startPage();
        List<BrmAcademicYear> list = brmAcademicYearService.selectBrmAcademicYearList(brmAcademicYear);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:year:export')")
    @Log(title = "学年", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmAcademicYear brmAcademicYear)
    {
        List<BrmAcademicYear> list = brmAcademicYearService.selectBrmAcademicYearList(brmAcademicYear);
        ExcelUtil<BrmAcademicYear> util = new ExcelUtil<BrmAcademicYear>(BrmAcademicYear.class);
        util.exportExcel(response, list, "学年数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:year:query')")
    @GetMapping(value = "/{yearId}")
    public AjaxResult getInfo(@PathVariable("yearId") Long yearId)
    {
        return success(brmAcademicYearService.selectBrmAcademicYearByYearId(yearId));
    }

    @PreAuthorize("@ss.hasPermi('brm:year:add')")
    @Log(title = "学年", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmAcademicYear brmAcademicYear)
    {
        return toAjax(brmAcademicYearService.insertBrmAcademicYear(brmAcademicYear));
    }

    @PreAuthorize("@ss.hasPermi('brm:year:edit')")
    @Log(title = "学年", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmAcademicYear brmAcademicYear)
    {
        return toAjax(brmAcademicYearService.updateBrmAcademicYear(brmAcademicYear));
    }

    @PreAuthorize("@ss.hasPermi('brm:year:remove')")
    @Log(title = "学年", businessType = BusinessType.DELETE)
    @DeleteMapping("/{yearIds}")
    public AjaxResult remove(@PathVariable Long[] yearIds)
    {
        return toAjax(brmAcademicYearService.deleteBrmAcademicYearByYearIds(yearIds));
    }
}
