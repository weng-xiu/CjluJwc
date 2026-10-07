package com.yu.web.controller.system;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.system.domain.SysDataClass;
import com.yu.system.service.ISysDataClassService;

/**
 * 数据分类分级台账 信息操作处理（K1 等保数据新规）
 *
 * 供数据安全管理员对业务表字段逐条登记数据类别、安全等级与保护措施，导出即合规交付物。
 *
 * @author yu
 * @date 2026-10-07
 */
@RestController
@RequestMapping("/system/dataClass")
public class SysDataClassController extends BaseController
{
    @Autowired
    private ISysDataClassService dataClassService;

    /**
     * 查询台账列表
     */
    @PreAuthorize("@ss.hasPermi('system:dataClass:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDataClass dataClass)
    {
        startPage();
        List<SysDataClass> list = dataClassService.selectDataClassList(dataClass);
        return getDataTable(list);
    }

    /**
     * 导出台账（合规交付物）
     */
    @Log(title = "数据分类分级", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:dataClass:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDataClass dataClass)
    {
        List<SysDataClass> list = dataClassService.selectDataClassList(dataClass);
        ExcelUtil<SysDataClass> util = new ExcelUtil<SysDataClass>(SysDataClass.class);
        util.exportExcel(response, list, "数据分类分级台账");
    }

    /**
     * 按编号获取登记详情
     */
    @PreAuthorize("@ss.hasPermi('system:dataClass:query')")
    @GetMapping(value = "/{classId}")
    public AjaxResult getInfo(@PathVariable Long classId)
    {
        return success(dataClassService.selectDataClassById(classId));
    }

    /**
     * 新增登记
     */
    @PreAuthorize("@ss.hasPermi('system:dataClass:add')")
    @Log(title = "数据分类分级", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDataClass dataClass)
    {
        if (!dataClassService.checkColumnUnique(dataClass))
        {
            return error("新增登记'" + dataClass.getTableName() + "." + dataClass.getColumnName() + "'失败，该表字段已登记");
        }
        dataClass.setCreateBy(getUsername());
        return toAjax(dataClassService.insertDataClass(dataClass));
    }

    /**
     * 修改登记
     */
    @PreAuthorize("@ss.hasPermi('system:dataClass:edit')")
    @Log(title = "数据分类分级", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDataClass dataClass)
    {
        if (!dataClassService.checkColumnUnique(dataClass))
        {
            return error("修改登记'" + dataClass.getTableName() + "." + dataClass.getColumnName() + "'失败，该表字段已登记");
        }
        dataClass.setUpdateBy(getUsername());
        return toAjax(dataClassService.updateDataClass(dataClass));
    }

    /**
     * 删除登记
     */
    @PreAuthorize("@ss.hasPermi('system:dataClass:remove')")
    @Log(title = "数据分类分级", businessType = BusinessType.DELETE)
    @DeleteMapping("/{classIds}")
    public AjaxResult remove(@PathVariable Long[] classIds)
    {
        return toAjax(dataClassService.deleteDataClassByIds(classIds));
    }
}
