package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.page.TableDataInfo;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.service.IAemExamPlanService;
import com.yu.aem.service.IAemExamInvigilationService;

/**
 * 考试安排与监考安排门户Controller
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/exam")
public class PortalExamController extends BaseController
{
    @Autowired
    private IAemExamPlanService aemExamPlanService;

    @Autowired
    private IAemExamInvigilationService aemExamInvigilationService;

    /** 学生端：考试安排查询 */
    @PreAuthorize("@ss.hasPermi('portal:exam:list')")
    @GetMapping("/list")
    public TableDataInfo list(AemExamPlan aemExamPlan)
    {
        startPage();
        List<AemExamPlan> list = aemExamPlanService.selectAemExamPlanList(aemExamPlan);
        return getDataTable(list);
    }

    /** 教师端：监考安排查询 */
    @PreAuthorize("@ss.hasPermi('portal:invigilation:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/invigilationList")
    public TableDataInfo invigilationList(AemExamInvigilation aemExamInvigilation)
    {
        startPage();
        List<AemExamInvigilation> list = aemExamInvigilationService.selectAemExamInvigilationList(aemExamInvigilation);
        return getDataTable(list);
    }
}
