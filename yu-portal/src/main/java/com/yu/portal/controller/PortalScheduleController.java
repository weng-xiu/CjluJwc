package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 课表查询门户Controller（学生课表 + 教师课表）
 *
 * @author ruoyi
 * @date 2026-05-20
 */
@RestController
@RequestMapping("/portal/schedule")
public class PortalScheduleController extends BaseController
{
    @Autowired
    private ITpmScheduleService tpmScheduleService;

    /** 学生端：查询个人课表 */
    @PreAuthorize("@ss.hasPermi('portal:schedule:list')")
    @GetMapping("/list")
    public TableDataInfo list(TpmSchedule tpmSchedule)
    {
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(tpmSchedule);
        return getDataTable(list);
    }

    /** 教师端：查询个人课表 */
    @PreAuthorize("@ss.hasPermi('portal:teacherSchedule:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/teacherList")
    public TableDataInfo teacherList(TpmSchedule tpmSchedule)
    {
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(tpmSchedule);
        return getDataTable(list);
    }

    /** 学生端：课表详情 */
    @GetMapping(value = "/detail")
    @PreAuthorize("@ss.hasPermi('portal:schedule:query')")
    public AjaxResult detail(TpmSchedule tpmSchedule)
    {
        return success(tpmScheduleService.selectTpmScheduleList(tpmSchedule));
    }
}
