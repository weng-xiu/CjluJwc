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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
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
    public AjaxResult add(@Validated @RequestBody SamCertificate samCertificate) { return toAjax(samCertificateService.insertSamCertificate(samCertificate)); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:edit')")
    @Log(title = "证书管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SamCertificate samCertificate) { return toAjax(samCertificateService.updateSamCertificate(samCertificate)); }

    @PreAuthorize("@ss.hasPermi('sam:certificate:remove')")
    @Log(title = "证书管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{certIds}")
    public AjaxResult remove(@PathVariable Long[] certIds) { return toAjax(samCertificateService.deleteSamCertificateByCertIds(certIds)); }

    /** S7a：按规则预生成唯一证书编号（新增对话框默认值/预览） */
    @PreAuthorize("@ss.hasPermi('sam:certificate:add')")
    @GetMapping("/previewNumber")
    public AjaxResult previewNumber(@RequestParam String certType, @RequestParam(required = false) Integer year)
    {
        return success(samCertificateService.previewCertNumber(certType, year));
    }

    /** S7a：批量生成证书（已通过审核且无该类型证书者，编号自动生成） */
    @PreAuthorize("@ss.hasPermi('sam:certificate:generate')")
    @Log(title = "证书管理", businessType = BusinessType.INSERT)
    @PostMapping("/batchGenerate")
    public AjaxResult batchGenerate(@RequestParam String certType, @RequestParam(required = false) String gradYear)
    {
        java.util.Map<String, Object> stat = samCertificateService.batchGenerateCertificates(certType, gradYear, getUsername());
        return AjaxResult.success("已生成 " + stat.get("generated") + " 份证书（候选 " + stat.get("candidates") + " 人）", stat);
    }

    /** 发放登记：置 is_issued=1、发放日期、领取人 */
    @PreAuthorize("@ss.hasPermi('sam:certificate:edit')")
    @Log(title = "证书管理", businessType = BusinessType.UPDATE)
    @PutMapping("/issue")
    public AjaxResult issue(@RequestBody IssueForm form)
    {
        SamCertificate cert = samCertificateService.selectSamCertificateByCertId(form.getCertId());
        if (cert == null) { return error("证书记录不存在"); }
        SamCertificate update = new SamCertificate();
        update.setCertId(form.getCertId());
        update.setIsIssued("1");
        update.setIssueDate(form.getIssueDate() != null ? form.getIssueDate() : new java.util.Date());
        update.setReceiver(form.getReceiver());
        return toAjax(samCertificateService.updateSamCertificate(update));
    }

    /** 发放登记表单 */
    public static class IssueForm
    {
        private Long certId;
        private String receiver;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private java.util.Date issueDate;
        public Long getCertId() { return certId; }
        public void setCertId(Long certId) { this.certId = certId; }
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        public java.util.Date getIssueDate() { return issueDate; }
        public void setIssueDate(java.util.Date issueDate) { this.issueDate = issueDate; }
    }
}
