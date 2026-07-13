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
}
