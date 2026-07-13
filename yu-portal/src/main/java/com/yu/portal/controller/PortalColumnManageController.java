package com.yu.portal.controller;

import java.util.List;
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
import com.yu.portal.domain.PortalColumn;
import com.yu.portal.service.IPortalColumnService;
import com.yu.common.core.page.TableDataInfo;

/**
 * 门户栏目管理Controller（后台管理）
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@RestController
@RequestMapping("/portal/columnManage")
public class PortalColumnManageController extends BaseController
{
    @Autowired
    private IPortalColumnService portalColumnService;

    @PreAuthorize("@ss.hasPermi('portal:column:list')")
    @GetMapping("/list")
    public TableDataInfo list(PortalColumn portalColumn)
    {
        startPage();
        List<PortalColumn> list = portalColumnService.selectPortalColumnList(portalColumn);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('portal:column:query')")
    @GetMapping(value = "/{columnId}")
    public AjaxResult getInfo(@PathVariable("columnId") Long columnId)
    {
        return success(portalColumnService.selectPortalColumnById(columnId));
    }

    @PreAuthorize("@ss.hasPermi('portal:column:add')")
    @Log(title = "门户栏目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalColumn portalColumn)
    {
        return toAjax(portalColumnService.insertPortalColumn(portalColumn));
    }

    @PreAuthorize("@ss.hasPermi('portal:column:edit')")
    @Log(title = "门户栏目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalColumn portalColumn)
    {
        return toAjax(portalColumnService.updatePortalColumn(portalColumn));
    }

    @PreAuthorize("@ss.hasPermi('portal:column:remove')")
    @Log(title = "门户栏目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{columnIds}")
    public AjaxResult remove(@PathVariable Long[] columnIds)
    {
        return toAjax(portalColumnService.deletePortalColumnByIds(columnIds));
    }
}
