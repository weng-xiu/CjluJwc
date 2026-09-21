package com.yu.brm.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.brm.service.IBrmResourceStatService;

/**
 * 资源利用分析Controller（B2）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@RestController
@RequestMapping("/brm/resourceStat")
public class BrmResourceStatController extends BaseController
{
    @Autowired
    private IBrmResourceStatService brmResourceStatService;

    /** 资源总览 */
    @PreAuthorize("@ss.hasPermi('brm:resourceStat:list')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) Long semesterId)
    {
        return success(brmResourceStatService.overview(semesterId));
    }

    /** 教室利用率明细 */
    @PreAuthorize("@ss.hasPermi('brm:resourceStat:list')")
    @GetMapping("/classroomUtilization")
    public AjaxResult classroomUtilization(@RequestParam(required = false) Long semesterId,
                                           @RequestParam(required = false) Long buildingId)
    {
        return success(brmResourceStatService.classroomUtilization(semesterId, buildingId));
    }

    /** 教师工作量明细 */
    @PreAuthorize("@ss.hasPermi('brm:resourceStat:list')")
    @GetMapping("/teacherWorkload")
    public AjaxResult teacherWorkload(@RequestParam(required = false) Long semesterId)
    {
        return success(brmResourceStatService.teacherWorkload(semesterId));
    }

    /** 维保到期提醒 */
    @PreAuthorize("@ss.hasPermi('brm:resourceStat:list')")
    @GetMapping("/maintenanceDue")
    public AjaxResult maintenanceDue()
    {
        return success(brmResourceStatService.maintenanceDue());
    }
}
