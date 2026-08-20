package com.yu.tpm.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 开课计划Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/offering")
public class TpmCourseOfferingController extends BaseController
{
    @Autowired
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @PreAuthorize("@ss.hasPermi('tpm:offering:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmCourseOffering tpmCourseOffering)
    {
        startPage();
        List<TpmCourseOffering> list = tpmCourseOfferingService.selectTpmCourseOfferingList(tpmCourseOffering);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:offering:export')")
    @Log(title = "开课计划", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmCourseOffering tpmCourseOffering)
    {
        List<TpmCourseOffering> list = tpmCourseOfferingService.selectTpmCourseOfferingList(tpmCourseOffering);
        ExcelUtil<TpmCourseOffering> util = new ExcelUtil<TpmCourseOffering>(TpmCourseOffering.class);
        util.exportExcel(response, list, "开课计划数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:offering:query')")
    @GetMapping(value = "/{offeringId}")
    public AjaxResult getInfo(@PathVariable("offeringId") Long offeringId)
    {
        return success(tpmCourseOfferingService.selectTpmCourseOfferingByOfferingId(offeringId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:offering:add')")
    @Log(title = "开课计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmCourseOffering tpmCourseOffering)
    {
        return toAjax(tpmCourseOfferingService.insertTpmCourseOffering(tpmCourseOffering));
    }

    @PreAuthorize("@ss.hasPermi('tpm:offering:edit')")
    @Log(title = "开课计划", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmCourseOffering tpmCourseOffering)
    {
        return toAjax(tpmCourseOfferingService.updateTpmCourseOffering(tpmCourseOffering));
    }

    @PreAuthorize("@ss.hasPermi('tpm:offering:remove')")
    @Log(title = "开课计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{offeringIds}")
    public AjaxResult remove(@PathVariable Long[] offeringIds)
    {
        return toAjax(tpmCourseOfferingService.deleteTpmCourseOfferingByOfferingIds(offeringIds));
    }

    /**
     * 确认开课
     */
    @PreAuthorize("@ss.hasPermi('tpm:offering:edit')")
    @Log(title = "开课计划", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{offeringId}")
    public AjaxResult confirm(@PathVariable Long offeringId)
    {
        return toAjax(tpmCourseOfferingService.confirmOffering(offeringId));
    }

    /**
     * 取消开课
     */
    @PreAuthorize("@ss.hasPermi('tpm:offering:edit')")
    @Log(title = "开课计划", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{offeringId}")
    public AjaxResult cancel(@PathVariable Long offeringId)
    {
        return toAjax(tpmCourseOfferingService.cancelOffering(offeringId));
    }
}
