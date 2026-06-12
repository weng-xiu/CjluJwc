package com.yu.brm.controller;

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
import com.yu.common.enums.BusinessType;
import com.yu.brm.domain.BrmTeacherQualification;
import com.yu.brm.service.IBrmTeacherQualificationService;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/brm/qualification")
public class BrmTeacherQualificationController extends BaseController
{
    @Autowired
    private IBrmTeacherQualificationService brmTeacherQualificationService;

    @PreAuthorize("@ss.hasPermi('brm:qualification:list')")
    @GetMapping("/list")
    public TableDataInfo list(BrmTeacherQualification brmTeacherQualification)
    {
        startPage();
        List<BrmTeacherQualification> list = brmTeacherQualificationService.selectBrmTeacherQualificationList(brmTeacherQualification);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('brm:qualification:export')")
    @Log(title = "教师授课资格", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BrmTeacherQualification brmTeacherQualification)
    {
        List<BrmTeacherQualification> list = brmTeacherQualificationService.selectBrmTeacherQualificationList(brmTeacherQualification);
        ExcelUtil<BrmTeacherQualification> util = new ExcelUtil<BrmTeacherQualification>(BrmTeacherQualification.class);
        util.exportExcel(response, list, "教师授课资格数据");
    }

    @PreAuthorize("@ss.hasPermi('brm:qualification:query')")
    @GetMapping(value = "/{qualId}")
    public AjaxResult getInfo(@PathVariable("qualId") Long qualId)
    {
        return success(brmTeacherQualificationService.selectBrmTeacherQualificationByQualId(qualId));
    }

    @PreAuthorize("@ss.hasPermi('brm:qualification:add')")
    @Log(title = "教师授课资格", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody BrmTeacherQualification brmTeacherQualification)
    {
        return toAjax(brmTeacherQualificationService.insertBrmTeacherQualification(brmTeacherQualification));
    }

    @PreAuthorize("@ss.hasPermi('brm:qualification:edit')")
    @Log(title = "教师授课资格", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody BrmTeacherQualification brmTeacherQualification)
    {
        return toAjax(brmTeacherQualificationService.updateBrmTeacherQualification(brmTeacherQualification));
    }

    @PreAuthorize("@ss.hasPermi('brm:qualification:remove')")
    @Log(title = "教师授课资格", businessType = BusinessType.DELETE)
    @DeleteMapping("/{qualIds}")
    public AjaxResult remove(@PathVariable Long[] qualIds)
    {
        return toAjax(brmTeacherQualificationService.deleteBrmTeacherQualificationByQualIds(qualIds));
    }
}
