package com.yu.portal.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.sam.service.ISamGraduationReviewService;

/**
 * 学生毕业预审门户Controller（S4：学生自助毕业预审，只读，强制绑定当前登录用户）
 *
 * @author ruoyi
 * @date 2026-09-20
 */
@RestController
@RequestMapping("/portal/graduation")
public class PortalGraduationController extends BaseController
{
    @Autowired
    private ISamGraduationReviewService samGraduationReviewService;

    /** 学生端：本人毕业资格自助预审（分项达成 + 差距清单，不落库） */
    @PreAuthorize("@ss.hasPermi('portal:graduation:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/preReview")
    public AjaxResult preReview()
    {
        return success(samGraduationReviewService.preReview(getUserId()));
    }
}
