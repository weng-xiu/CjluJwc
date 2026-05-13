package com.yu.sam.controller;

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
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/sam/certificate")
public class SamCertificateController extends BaseController
{
    @Autowired
    private ISamCertificateService samCertificateService;

    @PreAuthorize("@ss.hasPermi('sam:certificate:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamCertificate samCertificate) { startPage(); List<SamCertificate> list = samCertificateService.selectSamCertificateList(samCertificate); return getDataTable(list); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:export')")
    @Log(title = "证书管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamCertificate samCertificate) { List<SamCertificate> list = samCertificateService.selectSamCertificateList(samCertificate); ExcelUtil<SamCertificate> util = new ExcelUtil<SamCertificate>(SamCertificate.class); util.exportExcel(response, list, "证书管理数据"); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:query')")
    @GetMapping(value = "/{certId}")
    public AjaxResult getInfo(@PathVariable("certId") Long certId) { return success(samCertificateService.selectSamCertificateByCertId(certId)); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:add')")
    @Log(title = "证书管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SamCertificate samCertificate) { return toAjax(samCertificateService.insertSamCertificate(samCertificate)); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:edit')")
    @Log(title = "证书管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SamCertificate samCertificate) { return toAjax(samCertificateService.updateSamCertificate(samCertificate)); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:remove')")
    @Log(title = "证书管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{certIds}")
    public AjaxResult remove(@PathVariable Long[] certIds) { return toAjax(samCertificateService.deleteSamCertificateByCertIds(certIds)); }
}
