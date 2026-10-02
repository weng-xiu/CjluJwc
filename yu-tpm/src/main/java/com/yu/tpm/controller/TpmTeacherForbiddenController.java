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
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.tpm.domain.TpmTeacherForbidden;
import com.yu.tpm.service.ITpmTeacherForbiddenService;

/**
 * 教师禁排时间片Controller（F2-1）
 *
 * @author ruoyi
 * @date 2026-10-02
 */
@RestController
@RequestMapping("/tpm/teacherForbidden")
public class TpmTeacherForbiddenController extends BaseController
{
    @Autowired
    private ITpmTeacherForbiddenService tpmTeacherForbiddenService;

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmTeacherForbidden tpmTeacherForbidden)
    {
        startPage();
        List<TpmTeacherForbidden> list = tpmTeacherForbiddenService.selectTpmTeacherForbiddenList(tpmTeacherForbidden);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:export')")
    @Log(title = "教师禁排", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TpmTeacherForbidden tpmTeacherForbidden)
    {
        List<TpmTeacherForbidden> list = tpmTeacherForbiddenService.selectTpmTeacherForbiddenList(tpmTeacherForbidden);
        ExcelUtil<TpmTeacherForbidden> util = new ExcelUtil<TpmTeacherForbidden>(TpmTeacherForbidden.class);
        util.exportExcel(response, list, "教师禁排数据");
    }

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:query')")
    @GetMapping(value = "/{forbiddenId}")
    public AjaxResult getInfo(@PathVariable("forbiddenId") Long forbiddenId)
    {
        return success(tpmTeacherForbiddenService.selectTpmTeacherForbiddenByForbiddenId(forbiddenId));
    }

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:add')")
    @Log(title = "教师禁排", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TpmTeacherForbidden tpmTeacherForbidden)
    {
        return toAjax(tpmTeacherForbiddenService.insertTpmTeacherForbidden(tpmTeacherForbidden));
    }

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:edit')")
    @Log(title = "教师禁排", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TpmTeacherForbidden tpmTeacherForbidden)
    {
        return toAjax(tpmTeacherForbiddenService.updateTpmTeacherForbidden(tpmTeacherForbidden));
    }

    @PreAuthorize("@ss.hasPermi('tpm:teacherForbidden:remove')")
    @Log(title = "教师禁排", businessType = BusinessType.DELETE)
    @DeleteMapping("/{forbiddenIds}")
    public AjaxResult remove(@PathVariable Long[] forbiddenIds)
    {
        return toAjax(tpmTeacherForbiddenService.deleteTpmTeacherForbiddenByForbiddenIds(forbiddenIds));
    }
}
