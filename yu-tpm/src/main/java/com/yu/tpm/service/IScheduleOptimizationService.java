package com.yu.tpm.service;

import java.util.List;
import java.util.Map;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.domain.dto.AvailableClassroom;

/**
 * 排课优化Service接口
 *
 * @author ruoyi
 */
public interface IScheduleOptimizationService
{
    /**
     * 检测指定学期的所有排课冲突
     *
     * @param semesterId 学期ID
     * @return 冲突列表
     */
    List<ScheduleConflict> detectConflicts(Long semesterId);

    /**
     * 检查指定教室在某时间段是否可用
     *
     * @param classroomId 教室ID
     * @param weekDay     星期几（1-7）
     * @param startPeriod 开始节次
     * @param endPeriod   结束节次
     * @param startWeek   起始周
     * @param endWeek     结束周
     * @return true表示可用，false表示不可用（存在冲突）
     */
    boolean canAssignClassroom(Long classroomId, Integer weekDay, Integer startPeriod, Integer endPeriod, Integer startWeek, Integer endWeek);

    /**
     * 查询满足条件的可用教室列表
     *
     * @param minCapacity 最小容量要求
     * @param weekDay     星期几（1-7）
     * @param startPeriod 开始节次
     * @param endPeriod   结束节次
     * @param startWeek   起始周
     * @param endWeek     结束周
     * @return 可用教室列表
     */
    List<AvailableClassroom> findAvailableClassrooms(Integer minCapacity, Integer weekDay, Integer startPeriod, Integer endPeriod, Integer startWeek, Integer endWeek);

    /**
     * 自动分配教室（贪心算法：为未排教室的开课分配最佳教室）
     *
     * @param semesterId 学期ID
     * @return 分配结果统计，包含successCount、failCount、failReasons等
     */
    Map<String, Object> autoAssignClassrooms(Long semesterId);

    /**
     * 时间片自动排课（T1）：对已确认但尚无排课的开课，自动决定星期/节次/周次并分配教室，
     * 输出可行课表。硬约束：教室容量、教师不冲突、教室不冲突；软约束：教室类型匹配、
     * 跨校区同楼宇、周课时均衡、班级不连堂。支持预览（dryRun 不落库）与落库，
     * 落库结果 schedule_type=auto，可在排课管理中人工拖拽调整。
     *
     * @param semesterId        学期ID
     * @param dryRun            true 仅预览不落库
     * @param daysPerWeek       每周排课天数（null 用配置默认）
     * @param periodsPerDay     每天最大节次（null 用配置默认）
     * @param periodsPerSession 每次连堂节数（null 用配置默认）
     * @param totalWeeks        学期总周数（null 用配置默认）
     * @return 排课结果统计与明细（items/failReasons/scheduledOfferings 等）
     */
    Map<String, Object> autoScheduleTimetable(Long semesterId, boolean dryRun,
                                              Integer daysPerWeek, Integer periodsPerDay,
                                              Integer periodsPerSession, Integer totalWeeks);

    /**
     * T5 拖拽调整：检查将排课移动到目标时间段（星期/节次窗口，周次不变）是否产生冲突。
     * 检测三类：教室占用、教师占用、学生（按选课名单精确判定），均排除自身记录。
     *
     * @param scheduleId  被调整的排课ID
     * @param weekDay     目标星期几（1-7）
     * @param startPeriod 目标开始节次
     * @param endPeriod   目标结束节次
     * @return 冲突列表，空表示可安全移动
     */
    List<ScheduleConflict> checkTargetSlotConflicts(Long scheduleId, Integer weekDay, Integer startPeriod, Integer endPeriod);

    /**
     * T5 拖拽调整：将排课移动到目标时间段并落库。
     * 默认存在冲突即拒绝并回传冲突明细；force=true 时仅做参数合法性校验后强制移动。
     *
     * @param scheduleId  被调整的排课ID
     * @param weekDay     目标星期几（1-7）
     * @param startPeriod 目标开始节次
     * @param endPeriod   目标结束节次
     * @param force       是否忽略冲突强制移动
     * @return 结果 Map：success、conflicts（冲突明细）、schedule（调整后排课）
     */
    Map<String, Object> applyDragAdjust(Long scheduleId, Integer weekDay, Integer startPeriod, Integer endPeriod, boolean force);
}
