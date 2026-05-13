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
import com.yu.sam.domain.SamStudent;
import com.yu.sam.service.ISamStudentService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

/**
 * 学生学籍Controller
 *
 * @author ruoyi
 * @date 2026-05-13
 */
@RestController
@RequestMapping("/sam/student")
public class SamStudentController extends BaseController
{
    @Autowired
    private ISamStudentService samStudentService;

    @PreAuthorize("@ss.hasPermi('sam:student:list')")
    @GetMapping("/list")
    public TableDataInfo list(SamStudent samStudent)
    {
        startPage();
        List<SamStudent> list = samStudentService.selectSamStudentList(samStudent);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('sam:student:export')")
    @Log(title = "学生学籍", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SamStudent samStudent)
    {
        List<SamStudent> list = samStudentService.selectSamStudentList(samStudent);
        ExcelUtil<SamStudent> util = new ExcelUtil<SamStudent>(SamStudent.class);
        util.exportExcel(response, list, "学生学籍数据");
    }

    @PreAuthorize("@ss.hasPermi('sam:student:query')")
    @GetMapping(value = "/{studentId}")
    public AjaxResult getInfo(@PathVariable("studentId") Long studentId)
    {
        return success(samStudentService.selectSamStudentByStudentId(studentId));
    }

    @PreAuthorize("@ss.hasPermi('sam:student:add')")
    @Log(title = "学生学籍", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SamStudent samStudent)
    {
        return toAjax(samStudentService.insertSamStudent(samStudent));
    }

    @PreAuthorize("@ss.hasPermi('sam:student:edit')")
    @Log(title = "学生学籍", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SamStudent samStudent)
    {
        return toAjax(samStudentService.updateSamStudent(samStudent));
    }

    @PreAuthorize("@ss.hasPermi('sam:student:remove')")
    @Log(title = "学生学籍", businessType = BusinessType.DELETE)
    @DeleteMapping("/{studentIds}")
    public AjaxResult remove(@PathVariable Long[] studentIds)
    {
        return toAjax(samStudentService.deleteSamStudentByStudentIds(studentIds));
    }
}
