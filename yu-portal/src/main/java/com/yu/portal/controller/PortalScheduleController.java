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
import com.yu.common.exception.ServiceException;
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

    /** 学生端：查询个人课表（非管理员强制只查本人，admin 可查全量供排错） */
    @PreAuthorize("@ss.hasPermi('portal:schedule:list')")
    @RateLimiter(time = 60, count = 300, limitType = LimitType.IP)
    @GetMapping("/list")
    public TableDataInfo list(TpmSchedule tpmSchedule)
    {
        startPage();
        if (!SecurityUtils.isAdmin(getUserId()))
        {
            // S-5：此前直接透传客户端查询条件，学生不传参数即可遍历全校排课；
            // 现收敛为与 /myList 同口径，仅返回本人课表（保留客户端学期号作为筛选）。
            List<TpmSchedule> mine = tpmScheduleService.selectStudentScheduleList(getUserId(), tpmSchedule.getSemesterId());
            return getDataTable(mine);
        }
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

    /** 学生端：课表详情（仅本人任课/本人选课可见，admin 放行） */
    @GetMapping(value = "/detail")
    @PreAuthorize("@ss.hasPermi('portal:schedule:query')")
    public AjaxResult detail(TpmSchedule tpmSchedule)
    {
        Long scheduleId = tpmSchedule.getScheduleId();
        if (scheduleId == null)
        {
            throw new ServiceException("课表ID不能为空");
        }
        TpmSchedule schedule = tpmScheduleService.selectTpmScheduleByScheduleId(scheduleId);
        if (schedule == null)
        {
            throw new ServiceException("课表不存在");
        }
        if (!SecurityUtils.isAdmin(getUserId()) && !isVisibleToSelf(schedule))
        {
            throw new ServiceException("无权查看该课表", com.yu.common.constant.HttpStatus.FORBIDDEN);
        }
        // 归属通过后固定主键查询，丢弃客户端其余过滤条件，避免借条件拼接读取他人排课
        TpmSchedule pinned = new TpmSchedule();
        pinned.setScheduleId(scheduleId);
        return success(tpmScheduleService.selectTpmScheduleList(pinned));
    }

    /** 课表归属：教师本人任课，或学生本人已选课（按本人课表集合命中） */
    private boolean isVisibleToSelf(TpmSchedule schedule)
    {
        Long userId = getUserId();
        if (userId.equals(schedule.getTeacherId()))
        {
            return true;
        }
        List<TpmSchedule> mine = tpmScheduleService.selectStudentScheduleList(userId, schedule.getSemesterId());
        return mine != null && mine.stream().anyMatch(s -> schedule.getScheduleId().equals(s.getScheduleId()));
    }
}
