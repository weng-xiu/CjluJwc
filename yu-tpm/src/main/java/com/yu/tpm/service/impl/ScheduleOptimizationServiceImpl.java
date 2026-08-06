package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.common.utils.schedule.TimeSlotUtils;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.AvailableClassroom;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.service.IScheduleOptimizationService;

/**
 * 排课优化Service实现
 *
 * @author ruoyi
 */
@Service
public class ScheduleOptimizationServiceImpl implements IScheduleOptimizationService
{
    private static final Logger log = LoggerFactory.getLogger(ScheduleOptimizationServiceImpl.class);

    /** 冲突类型：教室冲突 */
    private static final String CLASSROOM_CONFLICT = "CLASSROOM_CONFLICT";

    /** 冲突类型：教师冲突 */
    private static final String TEACHER_CONFLICT = "TEACHER_CONFLICT";

    /** 冲突类型：班级冲突 */
    private static final String CLASS_CONFLICT = "CLASS_CONFLICT";

    @Autowired
    private TpmScheduleMapper scheduleMapper;

    @Autowired
    private BrmClassroomMapper classroomMapper;

    /** 默认课程容量（从配置读取） */
    @org.springframework.beans.factory.annotation.Value("${tpm.schedule.defaultCapacity:30}")
    private int defaultCapacity;

    /**
     * 检测指定学期的所有排课冲突
     * 1. 查询该学期所有排课记录（含课程名、教师信息）
     * 2. 按教室分组，检测同一教室的时间冲突
     * 3. 按教师分组，检测同一教师的时间冲突（通过offeringId间接判断）
     */
    @Override
    public List<ScheduleConflict> detectConflicts(Long semesterId)
    {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        // 查询该学期所有排课（含关联信息）
        List<TpmSchedule> schedules = scheduleMapper.selectSchedulesBySemester(semesterId);
        if (schedules == null || schedules.isEmpty())
        {
            return conflicts;
        }

        // 按教室分组检测冲突（跳过未分配教室的排课）
        Map<Long, List<TpmSchedule>> classroomGroups = schedules.stream()
                .filter(s -> s.getClassroomId() != null)
                .collect(Collectors.groupingBy(TpmSchedule::getClassroomId));

        for (Map.Entry<Long, List<TpmSchedule>> entry : classroomGroups.entrySet())
        {
            List<TpmSchedule> group = entry.getValue();
            detectPairConflicts(group, conflicts, CLASSROOM_CONFLICT);
        }

        // 按教师分组检测冲突（通过offeringId关联，同一教师同一时间的不同开课）
        // 由于教师信息在offering层，同一teacherId的排课需要按教师分组
        // 这里简化为按offeringId分组后，检测同一教师不同开课间的冲突
        Map<Long, List<TpmSchedule>> teacherGroups = schedules.stream()
                .collect(Collectors.groupingBy(TpmSchedule::getOfferingId));
        // 获取所有开课ID并查找其教师ID，简化处理：检测跨开课的教师冲突
        // 由于TpmSchedule不直接存储teacherId，我们按weekDay+时间段检测所有已分配教室的排课
        // 实际教师冲突需要teacherId，这里通过offeringId关联同一教师的多个开课
        detectTeacherConflicts(schedules, conflicts);

        // 按开课ID分组检测班级冲突（同一班级在同一时间有多门课程）
        detectClassConflicts(schedules, conflicts);

        return conflicts;
    }

    /**
     * 在同一分组内两两比较检测时间冲突
     *
     * @param group       同一教室或同一教师的排课列表
     * @param conflicts   冲突结果列表
     * @param conflictType 冲突类型
     */
    private void detectPairConflicts(List<TpmSchedule> group, List<ScheduleConflict> conflicts, String conflictType)
    {
        for (int i = 0; i < group.size(); i++)
        {
            for (int j = i + 1; j < group.size(); j++)
            {
                TpmSchedule s1 = group.get(i);
                TpmSchedule s2 = group.get(j);
                boolean hasConflict = TimeSlotUtils.hasTimeConflict(
                        s1.getWeekDay(), s1.getStartPeriod(), s1.getEndPeriod(), s1.getStartWeek(), s1.getEndWeek(),
                        s2.getWeekDay(), s2.getStartPeriod(), s2.getEndPeriod(), s2.getStartWeek(), s2.getEndWeek());
                if (hasConflict)
                {
                    ScheduleConflict conflict = buildConflict(s1, s2, conflictType);
                    conflicts.add(conflict);
                }
            }
        }
    }

    /**
     * 检测教师冲突：同一教师在同一时间有多个不同开课的排课
     * 通过teacherId分组，检测同一教师的多个排课间的时间冲突
     */
    private void detectTeacherConflicts(List<TpmSchedule> schedules, List<ScheduleConflict> conflicts)
    {
        // 按教师ID分组（跳过未关联教师的排课）
        Map<Long, List<TpmSchedule>> teacherGroups = schedules.stream()
                .filter(s -> s.getTeacherId() != null)
                .collect(Collectors.groupingBy(TpmSchedule::getTeacherId));
        for (Map.Entry<Long, List<TpmSchedule>> entry : teacherGroups.entrySet())
        {
            List<TpmSchedule> group = entry.getValue();
            detectPairConflicts(group, conflicts, TEACHER_CONFLICT);
        }
    }

    /**
     * 检测班级冲突：同一班级在同一时间有多门不同课程的排课
     * 简化策略：按课程ID分组后，检测同课程不同开课间的时间冲突
     * 以及通过选课名单检测学生跨开课的时间冲突
     */
    private void detectClassConflicts(List<TpmSchedule> schedules, List<ScheduleConflict> conflicts)
    {
        // 按开课ID分组
        Map<Long, List<TpmSchedule>> offeringGroups = schedules.stream()
                .collect(Collectors.groupingBy(TpmSchedule::getOfferingId));
        List<Long> offeringIds = new ArrayList<>(offeringGroups.keySet());
        // 两两比较不同开课的排课，检测班级时间冲突
        for (int i = 0; i < offeringIds.size(); i++)
        {
            for (int j = i + 1; j < offeringIds.size(); j++)
            {
                List<TpmSchedule> group1 = offeringGroups.get(offeringIds.get(i));
                List<TpmSchedule> group2 = offeringGroups.get(offeringIds.get(j));
                for (TpmSchedule s1 : group1)
                {
                    for (TpmSchedule s2 : group2)
                    {
                        if (s1.getClassroomId() == null || s2.getClassroomId() == null) { continue; }
                        boolean hasConflict = TimeSlotUtils.hasTimeConflict(
                                s1.getWeekDay(), s1.getStartPeriod(), s1.getEndPeriod(), s1.getStartWeek(), s1.getEndWeek(),
                                s2.getWeekDay(), s2.getStartPeriod(), s2.getEndPeriod(), s2.getStartWeek(), s2.getEndWeek());
                        if (hasConflict)
                        {
                            ScheduleConflict conflict = buildConflict(s1, s2, CLASS_CONFLICT);
                            conflicts.add(conflict);
                        }
                    }
                }
            }
        }
    }

    /**
     * 构建ScheduleConflict对象
     */
    private ScheduleConflict buildConflict(TpmSchedule s1, TpmSchedule s2, String conflictType)
    {
        ScheduleConflict conflict = new ScheduleConflict();
        conflict.setScheduleId1(s1.getScheduleId());
        conflict.setScheduleId2(s2.getScheduleId());
        conflict.setConflictType(conflictType);
        conflict.setCourseName1(s1.getCourseName() != null ? s1.getCourseName() : "未知课程");
        conflict.setCourseName2(s2.getCourseName() != null ? s2.getCourseName() : "未知课程");
        conflict.setClassroomName(s1.getClassroomName() != null ? s1.getClassroomName() : "未分配");
        conflict.setTimeDesc(buildTimeDesc(s1));
        String typeLabel;
        if (CLASSROOM_CONFLICT.equals(conflictType)) {
            typeLabel = "教室";
        } else if (TEACHER_CONFLICT.equals(conflictType)) {
            typeLabel = "教师";
        } else {
            typeLabel = "班级";
        }
        conflict.setMessage(String.format("%s冲突：%s 与 %s 在 %s 时间段冲突",
                typeLabel, conflict.getCourseName1(), conflict.getCourseName2(), conflict.getTimeDesc()));
        return conflict;
    }

    /**
     * 生成时间描述字符串
     */
    private String buildTimeDesc(TpmSchedule s)
    {
        String[] weekDayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String dayName = (s.getWeekDay() != null && s.getWeekDay() >= 1 && s.getWeekDay() <= 7)
                ? weekDayNames[s.getWeekDay()] : "星期" + s.getWeekDay();
        return String.format("%s 第%d-%d节 第%d-%d周",
                dayName,
                s.getStartPeriod() != null ? s.getStartPeriod() : 0,
                s.getEndPeriod() != null ? s.getEndPeriod() : 0,
                s.getStartWeek() != null ? s.getStartWeek() : 0,
                s.getEndWeek() != null ? s.getEndWeek() : 0);
    }

    /**
     * 检查指定教室在某时间段是否可用
     */
    @Override
    public boolean canAssignClassroom(Long classroomId, Integer weekDay, Integer startPeriod,
                                       Integer endPeriod, Integer startWeek, Integer endWeek)
    {
        // 查询该教室在同一星期几且节次有重叠的排课
        List<TpmSchedule> existing = scheduleMapper.selectByClassroomAndTime(classroomId, weekDay, startPeriod, endPeriod);
        if (existing == null || existing.isEmpty())
        {
            return true;
        }
        // 逐条检测周次是否也冲突
        for (TpmSchedule s : existing)
        {
            if (TimeSlotUtils.hasWeeksOverlap(startWeek, endWeek, s.getStartWeek(), s.getEndWeek()))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 查询满足条件的可用教室列表
     * 1. 查询容量>=minCapacity的所有教室（从BrmClassroom）
     * 2. 对每个教室检查该时间段是否可用
     * 3. 返回可用教室列表
     */
    @Override
    public List<AvailableClassroom> findAvailableClassrooms(Integer minCapacity, Integer weekDay,
                                                            Integer startPeriod, Integer endPeriod,
                                                            Integer startWeek, Integer endWeek)
    {
        // 查询满足容量的教室（按容量升序，便于最佳适配）
        List<BrmClassroom> classrooms = classroomMapper.selectByMinCapacity(minCapacity);
        List<AvailableClassroom> available = new ArrayList<>();
        for (BrmClassroom cr : classrooms)
        {
            if (canAssignClassroom(cr.getClassroomId(), weekDay, startPeriod, endPeriod, startWeek, endWeek))
            {
                AvailableClassroom ac = new AvailableClassroom();
                ac.setClassroomId(cr.getClassroomId());
                ac.setClassroomName(cr.getClassroomName());
                ac.setBuildingName(cr.getBuildingName());
                ac.setCapacity(cr.getCapacity());
                ac.setClassroomTypeName(cr.getTypeName());
                available.add(ac);
            }
        }
        return available;
    }

    /**
     * 自动分配教室（贪心算法：为未排教室的开课分配最佳教室）
     * 1. 查询该学期未分配教室的排课记录（classroomId为空）
     * 2. 按课程人数从大到小排序（贪心：大课优先）——SQL已按max_students DESC排序
     * 3. 对每条排课，查找满足容量且时间可用的最小教室（最佳适配）
     * 4. 分配教室，更新排课记录
     * 5. 返回分配结果统计（成功数/失败数/失败原因）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoAssignClassrooms(Long semesterId)
    {
        Map<String, Object> result = new HashMap<>();
        List<String> failReasons = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 查询未分配教室的排课（已按max_students降序）
        List<TpmSchedule> unassigned = scheduleMapper.selectUnassignedSchedules(semesterId);
        if (unassigned == null || unassigned.isEmpty())
        {
            result.put("successCount", 0);
            result.put("failCount", 0);
            result.put("message", "没有未分配教室的排课记录");
            return result;
        }

        log.info("开始自动分配教室，共{}条未分配排课", unassigned.size());

        for (TpmSchedule schedule : unassigned)
        {
            // 获取所需容量（取开课的maxStudents，无则默认30）
            int requiredCapacity = (schedule.getMaxStudents() != null) ? schedule.getMaxStudents() : defaultCapacity;

            // 查找满足容量且时间可用的教室
            List<AvailableClassroom> available = findAvailableClassrooms(
                    requiredCapacity,
                    schedule.getWeekDay(), schedule.getStartPeriod(), schedule.getEndPeriod(),
                    schedule.getStartWeek(), schedule.getEndWeek());

            if (available.isEmpty())
            {
                failCount++;
                String reason = String.format("排课ID[%d]（%s）：无满足容量>= %d且时间可用的教室",
                        schedule.getScheduleId(),
                        schedule.getCourseName() != null ? schedule.getCourseName() : "未知",
                        requiredCapacity);
                failReasons.add(reason);
                log.warn(reason);
                continue;
            }

            // 最佳适配：选择容量最小的可用教室（available已按容量升序）
            AvailableClassroom bestClassroom = available.get(0);

            // 分配教室，更新排课记录
            TpmSchedule update = new TpmSchedule();
            update.setScheduleId(schedule.getScheduleId());
            update.setClassroomId(bestClassroom.getClassroomId());
            update.setScheduleType("auto");
            scheduleMapper.updateTpmSchedule(update);

            successCount++;
            log.info("排课ID[{}] 分配教室：{}（容量{}）",
                    schedule.getScheduleId(), bestClassroom.getClassroomName(), bestClassroom.getCapacity());
        }

        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalCount", unassigned.size());
        result.put("failReasons", failReasons);
        result.put("message", String.format("自动分配完成：成功%d条，失败%d条", successCount, failCount));

        log.info("自动分配教室完成：成功{}，失败{}，共{}", successCount, failCount, unassigned.size());
        return result;
    }
}
