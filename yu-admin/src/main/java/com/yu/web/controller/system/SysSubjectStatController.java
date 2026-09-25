package com.yu.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.web.domain.SubjectReportVo;
import com.yu.web.service.ISubjectStatService;

/**
 * 主题分析Controller（P3：学生结构 / 成绩分析 / 师资分析 + 师生数据上报导出）
 *
 * 只读跨模块聚合，登录并按菜单权限访问；数据全部来自真实业务表。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
@RestController
@RequestMapping("/system/subjectStat")
public class SysSubjectStatController extends BaseController
{
    @Autowired
    private ISubjectStatService subjectStatService;

    /** 学生结构主题 */
    @PreAuthorize("@ss.hasPermi('system:subjectStat:list')")
    @GetMapping("/studentStructure")
    public AjaxResult studentStructure()
    {
        return success(subjectStatService.selectStudentStructure());
    }

    /** 成绩分析主题（学期可选） */
    @PreAuthorize("@ss.hasPermi('system:subjectStat:list')")
    @GetMapping("/gradeAnalysis")
    public AjaxResult gradeAnalysis(@RequestParam(required = false) Long semesterId)
    {
        return success(subjectStatService.selectGradeAnalysis(semesterId));
    }

    /** 师资分析主题（学期可选） */
    @PreAuthorize("@ss.hasPermi('system:subjectStat:list')")
    @GetMapping("/teacherStructure")
    public AjaxResult teacherStructure(@RequestParam(required = false) Long semesterId)
    {
        return success(subjectStatService.selectTeacherStructure(semesterId));
    }

    /** 师生数据上报导出（分院系汇总，Excel） */
    @PreAuthorize("@ss.hasPermi('system:subjectStat:export')")
    @Log(title = "师生数据上报", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, @RequestParam(required = false) Long semesterId)
    {
        List<SubjectReportVo> list = subjectStatService.selectReportByDept(semesterId);
        ExcelUtil<SubjectReportVo> util = new ExcelUtil<SubjectReportVo>(SubjectReportVo.class);
        util.exportExcel(response, list, "师生数据上报");
    }
}
