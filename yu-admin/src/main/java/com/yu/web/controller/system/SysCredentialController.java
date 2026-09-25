package com.yu.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.system.domain.SysPrintRecord;
import com.yu.system.service.ISysPrintService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 电子凭证管理Controller（P4）
 *
 * @author yu
 * @date 2026-09-25
 */
@RestController
@RequestMapping("/system/credential")
public class SysCredentialController extends BaseController
{
    @Autowired
    private ISysPrintService sysPrintService;

    /**
     * 查询凭证发放记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:credential:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPrintRecord sysPrintRecord)
    {
        startPage();
        List<SysPrintRecord> list = sysPrintService.selectRecordList(sysPrintRecord);
        return getDataTable(list);
    }

    /**
     * 导出凭证发放记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:credential:export')")
    @Log(title = "电子凭证记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPrintRecord sysPrintRecord)
    {
        List<SysPrintRecord> list = sysPrintService.selectRecordList(sysPrintRecord);
        ExcelUtil<SysPrintRecord> util = new ExcelUtil<SysPrintRecord>(SysPrintRecord.class);
        util.exportExcel(response, list, "电子凭证记录数据");
    }

    /**
     * 获取凭证发放记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:credential:query')")
    @GetMapping(value = "/{recordId}")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(sysPrintService.selectRecordById(recordId));
    }

    /**
     * 渲染凭证预览（不落库）
     */
    @PreAuthorize("@ss.hasPermi('system:credential:query')")
    @GetMapping("/render")
    public AjaxResult render(@RequestParam String bizType, @RequestParam Long bizId,
                             @RequestParam(required = false) Long semesterId)
    {
        return success((Object) sysPrintService.render(bizType, bizId, semesterId));
    }

    /**
     * 发放凭证（生成编号/验证码并落发放记录，返回凭证版 HTML）
     */
    @PreAuthorize("@ss.hasPermi('system:credential:issue')")
    @Log(title = "电子凭证发放", businessType = BusinessType.INSERT)
    @PostMapping("/issue")
    public AjaxResult issue(@RequestParam String bizType, @RequestParam Long bizId,
                            @RequestParam(required = false) Long semesterId)
    {
        return success((Object) sysPrintService.issue(bizType, bizId, semesterId, "0"));
    }

    /**
     * 批量发放凭证（GRADE 按学期 scopeId=semesterId；EXAM_TICKET 按考试 scopeId=examId）
     */
    @PreAuthorize("@ss.hasPermi('system:credential:issue')")
    @Log(title = "电子凭证批量发放", businessType = BusinessType.INSERT)
    @PostMapping("/batchIssue")
    public AjaxResult batchIssue(@RequestParam String bizType, @RequestParam Long scopeId)
    {
        return success(sysPrintService.batchIssue(bizType, scopeId));
    }

    /**
     * 重新打开已发放凭证（按记录绑定模板渲染）
     */
    @PreAuthorize("@ss.hasPermi('system:credential:query')")
    @GetMapping("/print/{recordId}")
    public AjaxResult print(@PathVariable("recordId") Long recordId)
    {
        return success((Object) sysPrintService.renderByRecord(recordId));
    }

    /**
     * 作废凭证
     */
    @PreAuthorize("@ss.hasPermi('system:credential:revoke')")
    @Log(title = "电子凭证作废", businessType = BusinessType.UPDATE)
    @PutMapping("/revoke/{recordId}")
    public AjaxResult revoke(@PathVariable("recordId") Long recordId)
    {
        return toAjax(sysPrintService.revokeRecord(recordId));
    }
}
