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
import com.yu.brm.domain.BrmSemester;
import com.yu.brm.service.IBrmSemesterService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/semester")
public class BrmSemesterController extends BaseController
{
    @Autowired
    private IBrmSemesterService brmSemesterService;

    @PreAuthorize("@ss.hasPermi('brm:semester:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmSemester brmSemester)
    {
        startPage();
        List<BrmSemester> list = brmSemesterService.selectBrmSemesterList(brmSemester);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:semester:export')")
    @Log(title = "学期", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmSemester brmSemester)
    {
        List<BrmSemester> list = brmSemesterService.selectBrmSemesterList(brmSemester);
        ExcelUtil<BrmSemester> util = new ExcelUtil<BrmSemester>(BrmSemester.class);
        util.exportExcel(response, list, "学期数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:semester:query')")
    @GetMapping(value = "/{semesterId}")
    public AjaxResult getInfo(@PathVariable("semesterId") Long semesterId)
    {
        return success(brmSemesterService.selectBrmSemesterBySemesterId(semesterId));
    }

    @PreAuthorize("@ss.hasPermi('brm:semester:add')")
    @Log(title = "学期", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmSemester brmSemester)
    {
        return toAjax(brmSemesterService.insertBrmSemester(brmSemester));
    }

    @PreAuthorize("@ss.hasPermi('brm:semester:edit')")
    @Log(title = "学期", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmSemester brmSemester)
    {
        return toAjax(brmSemesterService.updateBrmSemester(brmSemester));
    }

    @PreAuthorize("@ss.hasPermi('brm:semester:remove')")
    @Log(title = "学期", businessType = BusinessType.DELETE)
    @DeleteMapping("/{semesterIds}")
    public AjaxResult remove(@PathVariable Long[] semesterIds)
    {
        return toAjax(brmSemesterService.deleteBrmSemesterBySemesterIds(semesterIds));
    }
}
