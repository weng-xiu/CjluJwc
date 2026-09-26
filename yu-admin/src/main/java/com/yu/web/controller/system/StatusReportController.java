package com.yu.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.web.domain.StatusReportBatch;
import com.yu.web.service.IStatusReportService;

/**
 * 教育部状态数据上报Controller
 *
 * <p>提供「字段映射说明 → 数据预览核对 → 一键生成批次 → 报盘导出 → 标记上报」闭环。
 * 预览接口对身份证、手机号脱敏，导出接口输出原始值以满足上报真实性要求。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/system/statusReport")
public class StatusReportController extends BaseController
{
    @Autowired
    private IStatusReportService statusReportService;

    /** 字段映射说明（页面口径展示与预览列定义） */
    @PreAuthorize("@ss.hasPermi('system:statusReport:query')")
    @GetMapping("/fields")
    public AjaxResult fields(@RequestParam(required = false) String reportType)
    {
        return success(statusReportService.selectFieldList(reportType));
    }

    /** 上报范围院系下拉（自含查询，避免依赖基础资源模块权限） */
    @PreAuthorize("@ss.hasPermi('system:statusReport:query')")
    @GetMapping("/deptOptions")
    public AjaxResult deptOptions()
    {
        return success(statusReportService.selectDeptOptions());
    }

    /** 报盘数据预览（分页，敏感字段脱敏） */
    @PreAuthorize("@ss.hasPermi('system:statusReport:query')")
    @GetMapping("/preview")
    public TableDataInfo preview(@RequestParam String reportType,
            @RequestParam(required = false) String reportYear,
            @RequestParam(required = false) Long deptId)
    {
        startPage();
        List<?> list = statusReportService.selectReportDataForPreview(reportType, reportYear, deptId);
        return getDataTable(list);
    }

    /** 上报批次列表 */
    @PreAuthorize("@ss.hasPermi('system:statusReport:list')")
    @GetMapping("/batchList")
    public TableDataInfo batchList(StatusReportBatch query)
    {
        startPage();
        List<StatusReportBatch> list = statusReportService.selectBatchList(query);
        return getDataTable(list);
    }

    /** 一键生成上报批次 */
    @PreAuthorize("@ss.hasPermi('system:statusReport:generate')")
    @Log(title = "状态数据上报", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody StatusReportBatch batch)
    {
        return success(statusReportService.generateBatch(batch, getUsername()));
    }

    /** 报盘导出（按批次导出时同步回填批次导出状态） */
    @PreAuthorize("@ss.hasPermi('system:statusReport:export')")
    @Log(title = "状态数据上报", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String reportYear,
            @RequestParam(required = false) Long deptId)
    {
        statusReportService.exportExcel(response, reportType, reportYear, deptId, batchId, getUsername());
    }

    /** 标记批次已上报 */
    @PreAuthorize("@ss.hasPermi('system:statusReport:generate')")
    @Log(title = "状态数据上报", businessType = BusinessType.UPDATE)
    @PutMapping("/submit/{batchId}")
    public AjaxResult submit(@PathVariable Long batchId)
    {
        return toAjax(statusReportService.markSubmitted(batchId, getUsername()));
    }

    /** 作废批次 */
    @PreAuthorize("@ss.hasPermi('system:statusReport:remove')")
    @Log(title = "状态数据上报", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{batchId}")
    public AjaxResult cancel(@PathVariable Long batchId)
    {
        return toAjax(statusReportService.cancelBatch(batchId, getUsername()));
    }

    /** 删除批次留痕（已上报批次不允许删除） */
    @PreAuthorize("@ss.hasPermi('system:statusReport:remove')")
    @Log(title = "状态数据上报", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch/{batchId}")
    public AjaxResult removeBatch(@PathVariable Long batchId)
    {
        return toAjax(statusReportService.deleteBatchById(batchId));
    }
}
