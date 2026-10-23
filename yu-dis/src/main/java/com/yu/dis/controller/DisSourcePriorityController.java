package com.yu.dis.controller;

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
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.dis.arbitration.ArbitrationPreviewRequest;
import com.yu.dis.domain.DisSourcePriority;
import com.yu.dis.service.IDisSourcePriorityService;

/**
 * 主数据裁决矩阵 Controller（F5 主数据裁决前端化）。
 *
 * <p>数据源优先级配置的 CRUD + 试裁决预览；任一改动实时重载裁决策略，无需发版。
 *
 * @author F5
 */
@RestController
@RequestMapping("/dis/priority")
public class DisSourcePriorityController extends BaseController
{
    @Autowired
    private IDisSourcePriorityService disSourcePriorityService;

    @PreAuthorize("@ss.hasPermi('dis:priority:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisSourcePriority disSourcePriority)
    {
        startPage();
        List<DisSourcePriority> list = disSourcePriorityService.selectDisSourcePriorityList(disSourcePriority);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('dis:priority:export')")
    @Log(title = "主数据裁决优先级", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DisSourcePriority disSourcePriority)
    {
        List<DisSourcePriority> list = disSourcePriorityService.selectDisSourcePriorityList(disSourcePriority);
        ExcelUtil<DisSourcePriority> util = new ExcelUtil<DisSourcePriority>(DisSourcePriority.class);
        util.exportExcel(response, list, "裁决优先级数据");
    }

    @PreAuthorize("@ss.hasPermi('dis:priority:query')")
    @GetMapping(value = "/{priorityId}")
    public AjaxResult getInfo(@PathVariable("priorityId") Long priorityId)
    {
        return success(disSourcePriorityService.selectDisSourcePriorityByPriorityId(priorityId));
    }

    @PreAuthorize("@ss.hasPermi('dis:priority:add')")
    @Log(title = "主数据裁决优先级", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DisSourcePriority disSourcePriority)
    {
        disSourcePriority.setCreateBy(getUsername());
        return toAjax(disSourcePriorityService.insertDisSourcePriority(disSourcePriority));
    }

    @PreAuthorize("@ss.hasPermi('dis:priority:edit')")
    @Log(title = "主数据裁决优先级", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DisSourcePriority disSourcePriority)
    {
        disSourcePriority.setUpdateBy(getUsername());
        return toAjax(disSourcePriorityService.updateDisSourcePriority(disSourcePriority));
    }

    @PreAuthorize("@ss.hasPermi('dis:priority:remove')")
    @Log(title = "主数据裁决优先级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{priorityIds}")
    public AjaxResult remove(@PathVariable Long[] priorityIds)
    {
        return toAjax(disSourcePriorityService.deleteDisSourcePriorityByPriorityIds(priorityIds));
    }

    /** 试裁决：按当前优先级配置对多源候选裁决权威版本（纯预览，不落库）。 */
    @PreAuthorize("@ss.hasPermi('dis:priority:query')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody ArbitrationPreviewRequest request)
    {
        return success(disSourcePriorityService.preview(request));
    }
}
