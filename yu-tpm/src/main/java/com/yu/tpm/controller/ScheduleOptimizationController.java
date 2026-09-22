package com.yu.tpm.controller;

import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.enums.BusinessType;
import com.yu.tpm.domain.dto.AvailableClassroom;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.service.IScheduleOptimizationService;

/**
 * 排课优化Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/tpm/scheduleOpt")
public class ScheduleOptimizationController extends BaseController
{
    @Autowired
    private IScheduleOptimizationService scheduleOptimizationService;

    /**
     * 检测排课冲突
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:detectConflict')")
    @GetMapping("/detectConflicts")
    @Log(title = "排课冲突检测", businessType = BusinessType.OTHER)
    public AjaxResult detectConflicts(@RequestParam Long semesterId)
    {
        List<ScheduleConflict> conflicts = scheduleOptimizationService.detectConflicts(semesterId);
        return success(conflicts);
    }

    /**
     * 检查指定教室在某时间段是否可用
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:findClassroom')")
    @GetMapping("/canAssign")
    public AjaxResult canAssign(@RequestParam Long classroomId, @RequestParam Integer weekDay,
                                @RequestParam Integer startPeriod, @RequestParam Integer endPeriod,
                                @RequestParam Integer startWeek, @RequestParam Integer endWeek)
    {
        boolean canAssign = scheduleOptimizationService.canAssignClassroom(
                classroomId, weekDay, startPeriod, endPeriod, startWeek, endWeek);
        return success(canAssign);
    }

    /**
     * 查询可用教室
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:findClassroom')")
    @GetMapping("/availableClassrooms")
    public AjaxResult findAvailableClassrooms(@RequestParam Integer minCapacity, @RequestParam Integer weekDay,
                                              @RequestParam Integer startPeriod, @RequestParam Integer endPeriod,
                                              @RequestParam Integer startWeek, @RequestParam Integer endWeek)
    {
        List<AvailableClassroom> available = scheduleOptimizationService
                .findAvailableClassrooms(minCapacity, weekDay, startPeriod, endPeriod, startWeek, endWeek);
        return success(available);
    }

    /**
     * 自动分配教室
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:autoAssign')")
    @PostMapping("/autoAssign")
    @Log(title = "自动分配教室", businessType = BusinessType.UPDATE)
    public AjaxResult autoAssign(@RequestParam Long semesterId)
    {
        Map<String, Object> result = scheduleOptimizationService.autoAssignClassrooms(semesterId);
        return success(result);
    }

    /**
     * 时间片自动排课预览（T1）：对已确认且尚无排课的开课试排，仅返回结果不落库。
     * 周课时/节次/周数等参数可选，缺省取系统配置。
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:autoAssign')")
    @PostMapping("/autoSchedulePreview")
    public AjaxResult autoSchedulePreview(@RequestParam Long semesterId,
                                          @RequestParam(required = false) Integer daysPerWeek,
                                          @RequestParam(required = false) Integer periodsPerDay,
                                          @RequestParam(required = false) Integer periodsPerSession,
                                          @RequestParam(required = false) Integer totalWeeks)
    {
        Map<String, Object> result = scheduleOptimizationService.autoScheduleTimetable(
                semesterId, true, daysPerWeek, periodsPerDay, periodsPerSession, totalWeeks);
        return success(result);
    }

    /**
     * 时间片自动排课落库（T1）：按预览算法生成排课并写入（schedule_type=auto），可在排课管理中人工调整。
     */
    @PreAuthorize("@ss.hasPermi('tpm:schedule:autoAssign')")
    @PostMapping("/autoScheduleApply")
    @Log(title = "时间片自动排课", businessType = BusinessType.INSERT)
    public AjaxResult autoScheduleApply(@RequestParam Long semesterId,
                                        @RequestParam(required = false) Integer daysPerWeek,
                                        @RequestParam(required = false) Integer periodsPerDay,
                                        @RequestParam(required = false) Integer periodsPerSession,
                                        @RequestParam(required = false) Integer totalWeeks)
    {
        Map<String, Object> result = scheduleOptimizationService.autoScheduleTimetable(
                semesterId, false, daysPerWeek, periodsPerDay, periodsPerSession, totalWeeks);
        return success(result);
    }
}
