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
import com.yu.tpm.domain.dto.StudentScheduleSlot;
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

        if (schedules != null && !schedules.isEmpty())
        {
            // 按教室分组检测冲突（跳过未分配教室的排课）
            Map<Long, List<TpmSchedule>> classroomGroups = schedules.stream()
                    .filter(s -> s.getClassroomId() != null)
                    .collect(Collectors.groupingBy(TpmSchedule::getClassroomId));

            for (Map.Entry<Long, List<TpmSchedule>> entry : classroomGroups.entrySet())
            {
                List<TpmSchedule> group = entry.getValue();
                detectPairConflicts(group, conflicts, CLASSROOM_CONFLICT);
            }

            // 按教师分组检测冲突（同一教师同一时间的不同开课）
            detectTeacherConflicts(schedules, conflicts);
        }

        // 班级冲突：按"学生选课名单"精确判定（同一学生在不同开课的时间重叠）
        // 修正原按 offeringId 跨开课两两比较导致的误报，并覆盖未分配教室的排课
        detectClassConflictsByStudents(semesterId, conflicts);

        return conflicts;
    }

    /**
     * 在同一分组内检测时间冲突。
     * 优化：先按星期几分桶（TimeSlotUtils 仅在 weekDay 相同时判为冲突），
     * 桶内再两两比较，降低比较规模；对同一冲突对去重。
     *
     * @param group       同一教室或同一教师的排课列表
     * @param conflicts   冲突结果列表
     * @param conflictType 冲突类型
     */
    private void detectPairConflicts(List<TpmSchedule> group, List<ScheduleConflict> conflicts, String conflictType)
    {
        // 按星期几分桶，仅同星期几才可能冲突
        Map<Integer, List<TpmSchedule>> byDay = new java.util.HashMap<>();
        for (TpmSchedule s : group)
        {
            if (s.getWeekDay() == null) { continue; }
            byDay.computeIfAbsent(s.getWeekDay(), k -> new ArrayList<>()).add(s);
        }
        for (List<TpmSchedule> dayGroup : byDay.values())
        {
            for (int i = 0; i < dayGroup.size(); i++)
            {
                for (int j = i + 1; j < dayGroup.size(); j++)
                {
                    TpmSchedule s1 = dayGroup.get(i);
                    TpmSchedule s2 = dayGroup.get(j);
                    boolean hasConflict = TimeSlotUtils.hasTimeConflict(
                            s1.getWeekDay(), s1.getStartPeriod(), s1.getEndPeriod(), s1.getStartWeek(), s1.getEndWeek(),
                            s2.getWeekDay(), s2.getStartPeriod(), s2.getEndPeriod(), s2.getStartWeek(), s2.getEndWeek());
                    if (hasConflict)
                    {
                        conflicts.add(buildConflict(s1, s2, conflictType));
                    }
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
     * 班级冲突检测（T2 修正）：按"学生选课名单"精确判定。
     * 语义：同一学生在两门不同开课（offeringId 不同）的排课上时间重叠，即为班级/学生冲突。
     * 优点：
     *  - 消除按 offeringId 跨开课两两比较的误报（不同班级同时段上课不再误报）；
     *  - 不依赖 classroomId，覆盖未分配教室的排课（消除漏检）；
     *  - 按学生分组 + 星期几分桶，比较规模由全局 O(n²) 降为各学生自身课表内两两比较。
     * 同一排课对仅报告一次（去重）。
     */
    private void detectClassConflictsByStudents(Long semesterId, List<ScheduleConflict> conflicts)
    {
        List<StudentScheduleSlot> slots = scheduleMapper.selectStudentScheduleSlotsBySemester(semesterId);
        if (slots == null || slots.isEmpty())
        {
            return;
        }
        // 按学生分组
        Map<Long, List<StudentScheduleSlot>> byStudent = slots.stream()
                .filter(s -> s.getStudentId() != null)
                .collect(Collectors.groupingBy(StudentScheduleSlot::getStudentId));
        // 去重：同一排课对只报告一次
        java.util.Set<String> reported = new java.util.HashSet<>();
        for (List<StudentScheduleSlot> mine : byStudent.values())
        {
            // 按星期几分桶
            Map<Integer, List<StudentScheduleSlot>> byDay = new java.util.HashMap<>();
            for (StudentScheduleSlot s : mine)
            {
                if (s.getWeekDay() == null) { continue; }
                byDay.computeIfAbsent(s.getWeekDay(), k -> new ArrayList<>()).add(s);
            }
            for (List<StudentScheduleSlot> daySlots : byDay.values())
            {
                for (int i = 0; i < daySlots.size(); i++)
                {
                    for (int j = i + 1; j < daySlots.size(); j++)
                    {
                        StudentScheduleSlot a = daySlots.get(i);
                        StudentScheduleSlot b = daySlots.get(j);
                        // 同一开课的多条排课不算班级冲突
                        if (a.getOfferingId() != null && a.getOfferingId().equals(b.getOfferingId())) { continue; }
                        boolean conflict = TimeSlotUtils.hasTimeConflict(
                                a.getWeekDay(), a.getStartPeriod(), a.getEndPeriod(), a.getStartWeek(), a.getEndWeek(),
                                b.getWeekDay(), b.getStartPeriod(), b.getEndPeriod(), b.getStartWeek(), b.getEndWeek());
                        if (!conflict) { continue; }
                        long s1 = a.getScheduleId() == null ? 0 : a.getScheduleId();
                        long s2 = b.getScheduleId() == null ? 0 : b.getScheduleId();
                        String key = CLASS_CONFLICT + ":" + Math.min(s1, s2) + "-" + Math.max(s1, s2);
                        if (!reported.add(key)) { continue; }
                        conflicts.add(buildStudentConflict(a, b));
                    }
                }
            }
        }
    }

    /**
     * 构建学生/班级冲突（基于学生名单）。
     */
    private ScheduleConflict buildStudentConflict(StudentScheduleSlot a, StudentScheduleSlot b)
    {
        ScheduleConflict conflict = new ScheduleConflict();
        conflict.setScheduleId1(a.getScheduleId());
        conflict.setScheduleId2(b.getScheduleId());
        conflict.setConflictType(CLASS_CONFLICT);
        conflict.setCourseName1(a.getCourseName() != null ? a.getCourseName() : "未知课程");
        conflict.setCourseName2(b.getCourseName() != null ? b.getCourseName() : "未知课程");
        conflict.setClassroomName("学生[" + a.getStudentId() + "]");
        conflict.setTimeDesc(buildSlotTimeDesc(a));
        conflict.setMessage(String.format("班级冲突：学生[%d] 所选《%s》与《%s》在 %s 时间重叠",
                a.getStudentId(), conflict.getCourseName1(), conflict.getCourseName2(), conflict.getTimeDesc()));
        return conflict;
    }

    /**
     * 生成时间槽描述。
     */
    private String buildSlotTimeDesc(StudentScheduleSlot s)
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
