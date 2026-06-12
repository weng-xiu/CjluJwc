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
import com.yu.brm.domain.BrmClass;
import com.yu.brm.service.IBrmClassService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/clazz")
public class BrmClassController extends BaseController
{
    @Autowired
    private IBrmClassService brmClassService;

    @PreAuthorize("@ss.hasPermi('brm:class:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmClass brmClass)
    {
        startPage();
        List<BrmClass> list = brmClassService.selectBrmClassList(brmClass);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:class:export')")
    @Log(title = "班级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmClass brmClass)
    {
        List<BrmClass> list = brmClassService.selectBrmClassList(brmClass);
        ExcelUtil<BrmClass> util = new ExcelUtil<BrmClass>(BrmClass.class);
        util.exportExcel(response, list, "班级数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:class:query')")
    @GetMapping(value = "/{classId}")
    public AjaxResult getInfo(@PathVariable("classId") Long classId)
    {
        return success(brmClassService.selectBrmClassByClassId(classId));
    }

    @PreAuthorize("@ss.hasPermi('brm:class:add')")
    @Log(title = "班级", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmClass brmClass)
    {
        return toAjax(brmClassService.insertBrmClass(brmClass));
    }

    @PreAuthorize("@ss.hasPermi('brm:class:edit')")
    @Log(title = "班级", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmClass brmClass)
    {
        return toAjax(brmClassService.updateBrmClass(brmClass));
    }

    @PreAuthorize("@ss.hasPermi('brm:class:remove')")
    @Log(title = "班级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{classIds}")
    public AjaxResult remove(@PathVariable Long[] classIds)
    {
        return toAjax(brmClassService.deleteBrmClassByClassIds(classIds));
    }
}
