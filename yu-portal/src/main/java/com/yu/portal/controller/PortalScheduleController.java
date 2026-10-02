package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.RateLimiter;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.LimitType;
import com.yu.common.utils.SecurityUtils;
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

    /** 学生端：查询本人课表（选课记录→开课→排课，强制绑定当前学生） */
    @PreAuthorize("@ss.hasPermi('portal:schedule:list') and @ss.hasAnyRoles('admin,student')")
    // V4.0 §7.3/A1：选课日查课表尖峰，按 IP 限 300 次/分（移端多接口页面叠加调用需留余量）
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/myList")
    public TableDataInfo myList(@RequestParam(required = false) Long semesterId)
    {
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectStudentScheduleList(getUserId(), semesterId);
        return getDataTable(list);
    }

    /** 学生端：查询个人课表 */
    @PreAuthorize("@ss.hasPermi('portal:schedule:list')")
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/list")
    public TableDataInfo list(TpmSchedule tpmSchedule)
    {
        startPage();
        List<TpmSchedule> list = tpmScheduleService.selectTpmScheduleList(tpmSchedule);
        return getDataTable(list);
    }

    /** 教师端：查询个人课表（P6 口径：非管理员强制收敛为本人任课，避开看到全校课表） */
    @PreAuthorize("@ss.hasPermi('portal:teacherSchedule:list') and @ss.hasAnyRoles('admin,teacher')")
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/teacherList")
    public TableDataInfo teacherList(TpmSchedule tpmSchedule)
    {
        // V4.0 §6.1/N7：该接口原先不做身份收敛，任何 teacher 角色不传 teacherId 即可拿到全校排课；
        // 与 PortalTeachingTaskController#list 保持同一口径（非 admin 按登录教师ID过滤）。
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            tpmSchedule.setTeacherId(getUserId());
        }
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
