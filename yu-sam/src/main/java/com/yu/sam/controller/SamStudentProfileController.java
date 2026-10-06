package com.yu.sam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
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
}
