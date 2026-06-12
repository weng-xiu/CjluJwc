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
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.service.ITpmCreditStructureService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 学分结构Controller
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@RestController
@RequestMapping("/tpm/creditStruct")
public class TpmCreditStructureController extends BaseController
{
    @Autowired
    private ITpmCreditStructureService tpmCreditStructureService;

    @PreAuthorize("@ss.hasPermi('tpm:credit:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmCreditStructure tpmCreditStructure)
    {
        startPage();
        List<TpmCreditStructure> list = tpmCreditStructureService.selectTpmCreditStructureList(tpmCreditStructure);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:credit:export')")
    @Log(title = "学分结构", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmCreditStructure tpmCreditStructure)
    {
        List<TpmCreditStructure> list = tpmCreditStructureService.selectTpmCreditStructureList(tpmCreditStructure);
        ExcelUtil<TpmCreditStructure> util = new ExcelUtil<TpmCreditStructure>(TpmCreditStructure.class);
        util.exportExcel(response, list, "学分结构数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:credit:query')")
    @GetMapping(value = "/{structId}")
    public AjaxResult getInfo(@PathVariable("structId") Long structId)
    {
        return success(tpmCreditStructureService.selectTpmCreditStructureByStructId(structId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:credit:add')")
    @Log(title = "学分结构", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmCreditStructure tpmCreditStructure)
    {
        return toAjax(tpmCreditStructureService.insertTpmCreditStructure(tpmCreditStructure));
    }

    @PreAuthorize("@ss.hasPermi('tpm:credit:edit')")
    @Log(title = "学分结构", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmCreditStructure tpmCreditStructure)
    {
        return toAjax(tpmCreditStructureService.updateTpmCreditStructure(tpmCreditStructure));
    }

    @PreAuthorize("@ss.hasPermi('tpm:credit:remove')")
    @Log(title = "学分结构", businessType = BusinessType.DELETE)
    @DeleteMapping("/{structIds}")
    public AjaxResult remove(@PathVariable Long[] structIds)
    {
        return toAjax(tpmCreditStructureService.deleteTpmCreditStructureByStructIds(structIds));
    }
}
