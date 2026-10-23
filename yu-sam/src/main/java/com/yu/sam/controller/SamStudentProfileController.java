package com.yu.sam.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.sam.domain.dto.StudentProfileExportVo;
import com.yu.sam.service.ISamStudentProfileService;

/**
 * N6 学生数字档案全息画像控制器（只读聚合）。
 *
 * @author N6
 */
@RestController
@RequestMapping("/sam/studentProfile")
public class SamStudentProfileController extends BaseController
{
    @Autowired
    private ISamStudentProfileService studentProfileService;

    /** 生成指定学生的全息数字档案。 */
    @PreAuthorize("@ss.hasPermi('sam:studentProfile:query')")
    @GetMapping("/{studentId}")
    public AjaxResult profile(@PathVariable("studentId") Long studentId)
    {
        return success(studentProfileService.buildProfile(studentId));
    }

    /** 学生数字档案导出（F4）：多板块摊平为「板块/项目/内容」三列 Excel。 */
    @PreAuthorize("@ss.hasPermi('sam:studentProfile:export')")
    @Log(title = "学生数字档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{studentId}")
    public void export(HttpServletResponse response, @PathVariable("studentId") Long studentId)
    {
        List<StudentProfileExportVo> list = studentProfileService.buildProfileExport(studentId);
        ExcelUtil<StudentProfileExportVo> util = new ExcelUtil<StudentProfileExportVo>(StudentProfileExportVo.class);
        util.exportExcel(response, list, "学生数字档案");
    }
}
