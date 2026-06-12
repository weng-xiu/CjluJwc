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
import org.springframework.validation.annotation.Validated;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmClassroomType;
import com.yu.brm.service.IBrmClassroomTypeService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/roomtype")
public class BrmClassroomTypeController extends BaseController
{
    @Autowired
    private IBrmClassroomTypeService brmClassroomTypeService;

    @PreAuthorize("@ss.hasPermi('brm:roomtype:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmClassroomType brmClassroomType)
    {
        startPage();
        List<BrmClassroomType> list = brmClassroomTypeService.selectBrmClassroomTypeList(brmClassroomType);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:roomtype:export')")
    @Log(title = "教室类型", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmClassroomType brmClassroomType)
    {
        List<BrmClassroomType> list = brmClassroomTypeService.selectBrmClassroomTypeList(brmClassroomType);
        ExcelUtil<BrmClassroomType> util = new ExcelUtil<BrmClassroomType>(BrmClassroomType.class);
        util.exportExcel(response, list, "教室类型数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:roomtype:query')")
    @GetMapping(value = "/{typeId}")
    public AjaxResult getInfo(@PathVariable("typeId") Long typeId)
    {
        return success(brmClassroomTypeService.selectBrmClassroomTypeByTypeId(typeId));
    }

    @PreAuthorize("@ss.hasPermi('brm:roomtype:add')")
    @Log(title = "教室类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmClassroomType brmClassroomType)
    {
        return toAjax(brmClassroomTypeService.insertBrmClassroomType(brmClassroomType));
    }

    @PreAuthorize("@ss.hasPermi('brm:roomtype:edit')")
    @Log(title = "教室类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmClassroomType brmClassroomType)
    {
        return toAjax(brmClassroomTypeService.updateBrmClassroomType(brmClassroomType));
    }

    @PreAuthorize("@ss.hasPermi('brm:roomtype:remove')")
    @Log(title = "教室类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{typeIds}")
    public AjaxResult remove(@PathVariable Long[] typeIds)
    {
        return toAjax(brmClassroomTypeService.deleteBrmClassroomTypeByTypeIds(typeIds));
    }
}
