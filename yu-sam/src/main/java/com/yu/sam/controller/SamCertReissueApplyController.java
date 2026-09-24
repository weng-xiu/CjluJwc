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
import com.yu.sam.domain.SamCertReissueApply;
import com.yu.sam.service.ISamCertReissueApplyService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 证书补办申请Controller（S7b）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@RestController
@RequestMapping("/sam/certReissue")
public class SamCertReissueApplyController extends BaseController
{
    @Autowired
    private ISamCertReissueApplyService samCertReissueApplyService;

    @PreAuthorize("@ss.hasPermi('sam:certReissue:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamCertReissueApply samCertReissueApply)
    {
        startPage();
        List<SamCertReissueApply> list = samCertReissueApplyService.selectSamCertReissueApplyList(samCertReissueApply);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('sam:certReissue:export')")
    @Log(title = "证书补办", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamCertReissueApply samCertReissueApply)
    {
        List<SamCertReissueApply> list = samCertReissueApplyService.selectSamCertReissueApplyList(samCertReissueApply);
        ExcelUtil<SamCertReissueApply> util = new ExcelUtil<SamCertReissueApply>(SamCertReissueApply.class);
        util.exportExcel(response, list, "证书补办申请数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:certReissue:query')")
    @GetMapping(value = "/{applyId}")
    public AjaxResult getInfo(@PathVariable("applyId") Long applyId)
    {
        return success(samCertReissueApplyService.selectSamCertReissueApplyByApplyId(applyId));
    }

    /** 学生可补办的原证书列表（供申请对话框选择） */
    @PreAuthorize("@ss.hasPermi('sam:certReissue:add')")
    @GetMapping("/certsOfStudent")
    public AjaxResult certsOfStudent(@RequestParam Long studentId)
    {
        return success(samCertReissueApplyService.selectCertsForStudent(studentId));
    }

    @PreAuthorize("@ss.hasPermi('sam:certReissue:add')")
    @Log(title = "证书补办", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    public AjaxResult submit(@Validated @RequestBody SamCertReissueApply samCertReissueApply)
    {
        samCertReissueApply.setCreateBy(getUsername());
        return toAjax(samCertReissueApplyService.submitApply(samCertReissueApply));
    }

    /** 受理通过：自动生成补办证书 */
    @PreAuthorize("@ss.hasPermi('sam:certReissue:audit')")
    @Log(title = "证书补办", businessType = BusinessType.UPDATE)
    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody AuditForm form)
    {
        return toAjax(samCertReissueApplyService.approve(form.getApplyId(), form.getAuditOpinion(), getUsername()));
    }

    /** 驳回 */
    @PreAuthorize("@ss.hasPermi('sam:certReissue:audit')")
    @Log(title = "证书补办", businessType = BusinessType.UPDATE)
    @PutMapping("/reject")
    public AjaxResult reject(@RequestBody AuditForm form)
    {
        return toAjax(samCertReissueApplyService.reject(form.getApplyId(), form.getAuditOpinion(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('sam:certReissue:remove')")
    @Log(title = "证书补办", businessType = BusinessType.DELETE)
    @DeleteMapping("/{applyIds}")
    public AjaxResult remove(@PathVariable Long[] applyIds)
    {
        return toAjax(samCertReissueApplyService.deleteSamCertReissueApplyByApplyIds(applyIds));
    }

    /** 受理表单 */
    public static class AuditForm
    {
        private Long applyId;
        private String auditOpinion;
        public Long getApplyId() { return applyId; }
        public void setApplyId(Long applyId) { this.applyId = applyId; }
        public String getAuditOpinion() { return auditOpinion; }
        public void setAuditOpinion(String auditOpinion) { this.auditOpinion = auditOpinion; }
    }
}
