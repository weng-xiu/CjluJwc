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
import com.yu.brm.domain.BrmMajor;
import com.yu.brm.service.IBrmMajorService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/major")
public class BrmMajorController extends BaseController
{
    @Autowired
    private IBrmMajorService brmMajorService;

    @PreAuthorize("@ss.hasPermi('brm:major:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmMajor brmMajor)
    {
        startPage();
        List<BrmMajor> list = brmMajorService.selectBrmMajorList(brmMajor);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:major:export')")
    @Log(title = "专业", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmMajor brmMajor)
    {
        List<BrmMajor> list = brmMajorService.selectBrmMajorList(brmMajor);
        ExcelUtil<BrmMajor> util = new ExcelUtil<BrmMajor>(BrmMajor.class);
        util.exportExcel(response, list, "专业数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:major:query')")
    @GetMapping(value = "/{majorId}")
    public AjaxResult getInfo(@PathVariable("majorId") Long majorId)
    {
        return success(brmMajorService.selectBrmMajorByMajorId(majorId));
    }

    @PreAuthorize("@ss.hasPermi('brm:major:add')")
    @Log(title = "专业", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmMajor brmMajor)
    {
        return toAjax(brmMajorService.insertBrmMajor(brmMajor));
    }

    @PreAuthorize("@ss.hasPermi('brm:major:edit')")
    @Log(title = "专业", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmMajor brmMajor)
    {
        return toAjax(brmMajorService.updateBrmMajor(brmMajor));
    }

    @PreAuthorize("@ss.hasPermi('brm:major:remove')")
    @Log(title = "专业", businessType = BusinessType.DELETE)
    @DeleteMapping("/{majorIds}")
    public AjaxResult remove(@PathVariable Long[] majorIds)
    {
        return toAjax(brmMajorService.deleteBrmMajorByMajorIds(majorIds));
    }
}
