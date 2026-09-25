package com.yu.web.controller.system;

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
import com.yu.system.domain.SysPrintTemplate;
import com.yu.system.service.ISysPrintService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 打印凭证模板Controller（P4）
 *
 * @author yu
 * @date 2026-09-25
 */
@RestController
@RequestMapping("/system/printTemplate")
public class SysPrintTemplateController extends BaseController
{
    @Autowired
    private ISysPrintService sysPrintService;

    /**
     * 查询打印凭证模板列表
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPrintTemplate sysPrintTemplate)
    {
        startPage();
        List<SysPrintTemplate> list = sysPrintService.selectTemplateList(sysPrintTemplate);
        return getDataTable(list);
    }

    /**
     * 导出打印凭证模板列表
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:export')")
    @Log(title = "打印凭证模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPrintTemplate sysPrintTemplate)
    {
        List<SysPrintTemplate> list = sysPrintService.selectTemplateList(sysPrintTemplate);
        ExcelUtil<SysPrintTemplate> util = new ExcelUtil<SysPrintTemplate>(SysPrintTemplate.class);
        util.exportExcel(response, list, "打印凭证模板数据");
    }

    /**
     * 获取打印凭证模板详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:query')")
    @GetMapping(value = "/{templateId}")
    public AjaxResult getInfo(@PathVariable("templateId") Long templateId)
    {
        return success(sysPrintService.selectTemplateById(templateId));
    }

    /**
     * 新增打印凭证模板
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:add')")
    @Log(title = "打印凭证模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysPrintTemplate sysPrintTemplate)
    {
        sysPrintTemplate.setCreateBy(getUsername());
        return toAjax(sysPrintService.insertTemplate(sysPrintTemplate));
    }

    /**
     * 修改打印凭证模板
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:edit')")
    @Log(title = "打印凭证模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysPrintTemplate sysPrintTemplate)
    {
        sysPrintTemplate.setUpdateBy(getUsername());
        return toAjax(sysPrintService.updateTemplate(sysPrintTemplate));
    }

    /**
     * 删除打印凭证模板
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:remove')")
    @Log(title = "打印凭证模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        return toAjax(sysPrintService.deleteTemplateByIds(templateIds));
    }

    /**
     * 模板测试渲染（示例数据，不落库）
     */
    @PreAuthorize("@ss.hasPermi('system:printTemplate:query')")
    @GetMapping("/preview/{templateId}")
    public AjaxResult preview(@PathVariable("templateId") Long templateId)
    {
        return success((Object) sysPrintService.previewTemplate(templateId));
    }
}
