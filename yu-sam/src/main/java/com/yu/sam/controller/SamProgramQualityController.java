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
import com.yu.sam.domain.dto.ProgramQualityExportVo;
import com.yu.sam.service.ISamProgramQualityService;

/**
 * F3 专业质量档案一页纸控制器（跨模块只读聚合）。
 *
 * @author F3
 */
@RestController
@RequestMapping("/sam/programQuality")
public class SamProgramQualityController extends BaseController
{
    @Autowired
    private ISamProgramQualityService programQualityService;

    /** 可选专业清单（档案页专业下拉）。 */
    @PreAuthorize("@ss.hasPermi('sam:programQuality:list')")
    @GetMapping("/majors")
    public AjaxResult majors()
    {
        return success(programQualityService.listMajors());
    }

    /** 生成指定专业的质量档案一页纸。 */
    @PreAuthorize("@ss.hasPermi('sam:programQuality:query')")
    @GetMapping("/{majorId}")
    public AjaxResult profile(@PathVariable("majorId") Long majorId)
    {
        return success(programQualityService.buildProfile(majorId));
    }

    /** 专业质量档案导出：多板块摊平为「板块/项目/内容」三列 Excel。 */
    @PreAuthorize("@ss.hasPermi('sam:programQuality:export')")
    @Log(title = "专业质量档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export/{majorId}")
    public void export(HttpServletResponse response, @PathVariable("majorId") Long majorId)
    {
        List<ProgramQualityExportVo> list = programQualityService.buildProfileExport(majorId);
        ExcelUtil<ProgramQualityExportVo> util = new ExcelUtil<ProgramQualityExportVo>(ProgramQualityExportVo.class);
        util.exportExcel(response, list, "专业质量档案");
    }
}
