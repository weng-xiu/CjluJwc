package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.schedule.TimeSlotUtils;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.AvailableClassroom;
import com.yu.tpm.domain.dto.AutoScheduleItem;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import com.yu.tpm.domain.dto.SchedulableClassroom;
import com.yu.tpm.domain.dto.ScheduleConflict;
import com.yu.tpm.domain.dto.StudentScheduleSlot;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
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

    @Autowired
    private TpmCourseOfferingMapper courseOfferingMapper;

    /** 默认课程容量（从配置读取） */
    @org.springframework.beans.factory.annotation.Value("${tpm.schedule.defaultCapacity:30}")
    private int defaultCapacity;

    /** T1 自动排课：每周排课天数 */
    @org.springframework.beans.factory.annotation.Value("${tpm.autoSchedule.daysPerWeek:5}")
    private int cfgDaysPerWeek;

    /** T1 自动排课：每天最大节次 */
    @org.springframework.beans.factory.annotation.Value("${tpm.autoSchedule.periodsPerDay:8}")
    private int cfgPeriodsPerDay;

    /** T1 自动排课：每次连堂节数 */
    @org.springframework.beans.factory.annotation.Value("${tpm.autoSchedule.periodsPerSession:2}")
    private int cfgPeriodsPerSession;

    /** T1 自动排课：学期总周数 */
    @org.springframework.beans.factory.annotation.Value("${tpm.autoSchedule.totalWeeks:16}")
    private int cfgTotalWeeks;

    /** T1 自动排课：无学时数据时默认周课时 */
    @org.springframework.beans.factory.annotation.Value("${tpm.autoSchedule.defaultWeeklyHours:2}")
    private int cfgDefaultWeeklyHours;

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

    /**
     * 时间片自动排课引擎（T1）：对已确认但尚无排课的开课，自动决定星期/节次/周次并分配教室。
     * 采用"约束贪心 + 占用网格"构造式算法：按难度（容量）降序处理开课，逐次课在空闲时间片
     * 中择优（教师不冲突、教室不冲突为硬约束；类型/校区/楼宇/容量贴合、周课时均衡为软约束评分）。
     * 以 (星期,节次) 二维网格保守屏蔽占用，并用学期内已有排课预热，保证与手动课表零硬冲突。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> autoScheduleTimetable(Long semesterId, boolean dryRun,
                                                     Integer daysPerWeek, Integer periodsPerDay,
                                                     Integer periodsPerSession, Integer totalWeeks)
    {
        Map<String, Object> result = new HashMap<>();
        int days = clamp(daysPerWeek != null ? daysPerWeek : cfgDaysPerWeek, 1, 7);
        int ppd = clamp(periodsPerDay != null ? periodsPerDay : cfgPeriodsPerDay, 1, 20);
        int pps = clamp(periodsPerSession != null ? periodsPerSession : cfgPeriodsPerSession, 1, ppd);
        int weeks = clamp(totalWeeks != null ? totalWeeks : cfgTotalWeeks, 1, 60);
        result.put("daysPerWeek", days);
        result.put("periodsPerDay", ppd);
        result.put("periodsPerSession", pps);
        result.put("totalWeeks", weeks);

        List<ScheduleCandidate> candidates = courseOfferingMapper.selectOfferingsToSchedule(semesterId);
        if (candidates == null || candidates.isEmpty())
        {
            result.put("totalCandidates", 0);
            result.put("scheduledOfferings", 0);
            result.put("failedOfferings", 0);
            result.put("totalSessions", 0);
            result.put("items", new ArrayList<>());
            result.put("failReasons", new ArrayList<>());
            result.put("dryRun", dryRun);
            result.put("message", "没有需要自动排课的开课（已确认且尚无排课记录）");
            return result;
        }

        List<SchedulableClassroom> rooms = scheduleMapper.selectSchedulableClassrooms();
        if (rooms == null || rooms.isEmpty())
        {
            List<String> fr = new ArrayList<>();
            fr.add("无可用教室（brm_classroom 无状态正常记录）");
            result.put("totalCandidates", candidates.size());
            result.put("scheduledOfferings", 0);
            result.put("failedOfferings", candidates.size());
            result.put("totalSessions", 0);
            result.put("items", new ArrayList<>());
            result.put("failReasons", fr);
            result.put("dryRun", dryRun);
            result.put("message", "自动排课失败：没有可用教室");
            return result;
        }

        // 占用网格：教室、教师。索引 [day(1..days)][period(1..ppd)]，保守按 (星期,节次) 屏蔽（忽略周次差异以确保零硬冲突）
        Map<Long, boolean[][]> roomGrid = new HashMap<>();
        Map<Long, boolean[][]> teacherGrid = new HashMap<>();
        List<TpmSchedule> existing = scheduleMapper.selectSchedulesBySemester(semesterId);
        if (existing != null)
        {
            for (TpmSchedule s : existing)
            {
                if (s.getWeekDay() == null || s.getStartPeriod() == null || s.getEndPeriod() == null) { continue; }
                int day = s.getWeekDay();
                if (day < 1 || day > days) { continue; }
                int sp = Math.max(1, s.getStartPeriod());
                int ep = Math.min(ppd, s.getEndPeriod());
                if (s.getClassroomId() != null)
                {
                    markOccupied(roomGrid.computeIfAbsent(s.getClassroomId(), k -> newGrid(days, ppd)), day, sp, ep);
                }
                if (s.getTeacherId() != null)
                {
                    markOccupied(teacherGrid.computeIfAbsent(s.getTeacherId(), k -> newGrid(days, ppd)), day, sp, ep);
                }
            }
        }

        Map<Long, int[]> teacherDayLoad = new HashMap<>();
        List<AutoScheduleItem> items = new ArrayList<>();
        List<String> failReasons = new ArrayList<>();
        int scheduledOfferings = 0;
        int failedOfferings = 0;

        for (ScheduleCandidate c : candidates)
        {
            int reqCap = (c.getMaxStudents() != null && c.getMaxStudents() > 0) ? c.getMaxStudents() : defaultCapacity;
            int weeklyHours = deriveWeeklyHours(c, weeks);
            int sessions = computeSessions(weeklyHours, pps, days);
            boolean needsLab = c.getPracticeHours() != null && c.getPracticeHours() > 0;

            Set<Integer> usedDays = new HashSet<>();
            Long primaryBuilding = null;
            int placed = 0;
            for (int sIdx = 0; sIdx < sessions; sIdx++)
            {
                List<Integer> dayOrder = orderDays(days, usedDays, c.getTeacherId(), teacherDayLoad);
                AutoScheduleItem item = null;
                outer:
                for (Integer day : dayOrder)
                {
                    for (int start = 1; start + pps - 1 <= ppd; start++)
                    {
                        int end = start + pps - 1;
                        if (c.getTeacherId() != null && !blockFree(teacherGrid.get(c.getTeacherId()), day, start, end))
                        {
                            continue;
                        }
                        SchedulableClassroom best = chooseClassroom(rooms, roomGrid, day, start, end,
                                reqCap, needsLab, c.getCampusId(), primaryBuilding);
                        if (best != null)
                        {
                            item = new AutoScheduleItem();
                            item.setOfferingId(c.getOfferingId());
                            item.setCourseName(c.getCourseName());
                            item.setTeacherName(c.getTeacherName());
                            item.setWeekDay(day);
                            item.setStartPeriod(start);
                            item.setEndPeriod(end);
                            item.setStartWeek(1);
                            item.setEndWeek(weeks);
                            item.setClassroomId(best.getClassroomId());
                            item.setClassroomName(best.getClassroomName());
                            item.setNote(buildNote(needsLab, best, c.getCampusId(), primaryBuilding, reqCap));
                            break outer;
                        }
                    }
                }
                if (item == null)
                {
                    break;
                }
                markOccupied(roomGrid.computeIfAbsent(item.getClassroomId(), k -> newGrid(days, ppd)),
                        item.getWeekDay(), item.getStartPeriod(), item.getEndPeriod());
                if (c.getTeacherId() != null)
                {
                    markOccupied(teacherGrid.computeIfAbsent(c.getTeacherId(), k -> newGrid(days, ppd)),
                            item.getWeekDay(), item.getStartPeriod(), item.getEndPeriod());
                    teacherDayLoad.computeIfAbsent(c.getTeacherId(), k -> new int[days + 1])[item.getWeekDay()]++;
                }
                usedDays.add(item.getWeekDay());
                if (primaryBuilding == null)
                {
                    SchedulableClassroom r = findRoomById(rooms, item.getClassroomId());
                    if (r != null) { primaryBuilding = r.getBuildingId(); }
                }
                items.add(item);
                placed++;
            }

            String label = c.getCourseName() != null ? c.getCourseName() : ("ID" + c.getOfferingId());
            if (placed == 0)
            {
                failedOfferings++;
                failReasons.add(String.format("开课[%s]（需容量%d，%s）：无满足约束的可用时间片/教室",
                        label, reqCap, needsLab ? "实践类" : "理论类"));
            }
            else
            {
                scheduledOfferings++;
                if (placed < sessions)
                {
                    failReasons.add(String.format("开课[%s]：计划%d次/周，实际排入%d次（后续会话无可用时间片）",
                            label, sessions, placed));
                }
            }
        }

        int inserted = 0;
        if (!dryRun)
        {
            String op = safeUsername();
            for (AutoScheduleItem it : items)
            {
                TpmSchedule tpmSchedule = new TpmSchedule();
                tpmSchedule.setOfferingId(it.getOfferingId());
                tpmSchedule.setClassroomId(it.getClassroomId());
                tpmSchedule.setWeekDay(it.getWeekDay());
                tpmSchedule.setStartPeriod(it.getStartPeriod());
                tpmSchedule.setEndPeriod(it.getEndPeriod());
                tpmSchedule.setStartWeek(it.getStartWeek());
                tpmSchedule.setEndWeek(it.getEndWeek());
                tpmSchedule.setScheduleType("auto");
                tpmSchedule.setStatus("0");
                tpmSchedule.setCreateBy(op);
                tpmSchedule.setCreateTime(DateUtils.getNowDate());
                inserted += scheduleMapper.insertTpmSchedule(tpmSchedule);
            }
        }

        result.put("totalCandidates", candidates.size());
        result.put("scheduledOfferings", scheduledOfferings);
        result.put("failedOfferings", failedOfferings);
        result.put("totalSessions", items.size());
        result.put("inserted", dryRun ? 0 : inserted);
        result.put("dryRun", dryRun);
        result.put("items", items);
        result.put("failReasons", failReasons);
        result.put("message", String.format("%s：%d个开课待排，成功编排%d个（共%d次课），失败/未满%d个%s",
                dryRun ? "预览" : "自动排课",
                candidates.size(), scheduledOfferings, items.size(), failedOfferings,
                dryRun ? "（未落库）" : ("，已写入" + inserted + "条排课")));
        log.info("T1自动排课：学期={} 候选={} 成功={} 会话={} 失败={} dryRun={}",
                semesterId, candidates.size(), scheduledOfferings, items.size(), failedOfferings, dryRun);
        return result;
    }

    // ================= T1 自动排课辅助方法 =================

    /** 数值裁剪到 [min,max] */
    private int clamp(int v, int min, int max)
    {
        if (v < min) { return min; }
        if (v > max) { return max; }
        return v;
    }

    /** 新建 [days+1][ppd+1] 占用网格（1 基索引） */
    private boolean[][] newGrid(int days, int ppd)
    {
        return new boolean[days + 1][ppd + 1];
    }

    /** 标记节次区间占用 */
    private void markOccupied(boolean[][] grid, int day, int start, int end)
    {
        for (int p = start; p <= end; p++)
        {
            if (day >= 0 && day < grid.length && p >= 1 && p < grid[day].length)
            {
                grid[day][p] = true;
            }
        }
    }

    /** 判断节次区间是否空闲（grid 为 null 视为空闲） */
    private boolean blockFree(boolean[][] grid, int day, int start, int end)
    {
        if (grid == null) { return true; }
        for (int p = start; p <= end; p++)
        {
            if (day >= 0 && day < grid.length && p >= 1 && p < grid[day].length && grid[day][p])
            {
                return false;
            }
        }
        return true;
    }

    /** 由总学时与周数推导周课时；无学时数据取默认周课时 */
    private int deriveWeeklyHours(ScheduleCandidate c, int weeks)
    {
        int wh;
        if (c.getTotalHours() != null && c.getTotalHours() > 0 && weeks > 0)
        {
            wh = (int) Math.round(c.getTotalHours() / (double) weeks);
        }
        else
        {
            wh = cfgDefaultWeeklyHours;
        }
        return wh < 1 ? 1 : wh;
    }

    /** 由周课时推导每周会话数（每会话 pps 节），并裁剪到 [1,days] */
    private int computeSessions(int weeklyHours, int pps, int days)
    {
        int sessions = (int) Math.ceil(weeklyHours / (double) pps);
        if (sessions < 1) { sessions = 1; }
        if (sessions > days) { sessions = days; }
        return sessions;
    }

    /** 择日顺序：优先未用过的天（不连堂/铺开），再按教师当日负载升序、日期升序（周课时均衡） */
    private List<Integer> orderDays(int days, Set<Integer> usedDays, Long teacherId, Map<Long, int[]> teacherDayLoad)
    {
        List<Integer> order = new ArrayList<>();
        for (int d = 1; d <= days; d++) { order.add(d); }
        final int[] load = teacherId != null ? teacherDayLoad.get(teacherId) : null;
        order.sort((a, b) -> {
            int ua = usedDays.contains(a) ? 1 : 0;
            int ub = usedDays.contains(b) ? 1 : 0;
            if (ua != ub) { return ua - ub; }
            int la = load != null ? load[a] : 0;
            int lb = load != null ? load[b] : 0;
            if (la != lb) { return la - lb; }
            return a - b;
        });
        return order;
    }

    /** 在满足容量且时段空闲的教室中按软约束评分择优（类型匹配 > 同校区 > 同楼宇 > 容量贴合） */
    private SchedulableClassroom chooseClassroom(List<SchedulableClassroom> rooms, Map<Long, boolean[][]> roomGrid,
                                                 int day, int start, int end, int reqCap, boolean needsLab,
                                                 Long campusId, Long primaryBuilding)
    {
        SchedulableClassroom best = null;
        int bestScore = Integer.MIN_VALUE;
        for (SchedulableClassroom r : rooms)
        {
            if (r.getCapacity() == null || r.getCapacity() < reqCap) { continue; }
            if (!blockFree(roomGrid.get(r.getClassroomId()), day, start, end)) { continue; }
            boolean lab = isLabRoom(r.getTypeName());
            int score;
            if (needsLab) { score = lab ? 100 : -20; }
            else { score = lab ? -10 : 5; }
            if (campusId != null && campusId.equals(r.getCampusId())) { score += 50; }
            if (primaryBuilding != null && primaryBuilding.equals(r.getBuildingId())) { score += 30; }
            score -= (r.getCapacity() - reqCap) / 10;
            if (score > bestScore)
            {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    /** 依据教室类型名称判断是否实验/机房类 */
    private boolean isLabRoom(String typeName)
    {
        if (typeName == null) { return false; }
        return typeName.contains("实验") || typeName.contains("机房") || typeName.contains("实践")
                || typeName.contains("实训") || typeName.contains("计算");
    }

    private SchedulableClassroom findRoomById(List<SchedulableClassroom> rooms, Long id)
    {
        if (id == null) { return null; }
        for (SchedulableClassroom r : rooms)
        {
            if (id.equals(r.getClassroomId())) { return r; }
        }
        return null;
    }

    private String buildNote(boolean needsLab, SchedulableClassroom room, Long campusId, Long primaryBuilding, int reqCap)
    {
        StringBuilder sb = new StringBuilder();
        if (needsLab && isLabRoom(room.getTypeName())) { sb.append("实验/机房匹配; "); }
        if (campusId != null && campusId.equals(room.getCampusId())) { sb.append("同校区; "); }
        if (primaryBuilding != null && primaryBuilding.equals(room.getBuildingId())) { sb.append("同楼宇; "); }
        sb.append("容量").append(room.getCapacity()).append("≥").append(reqCap);
        return sb.toString();
    }

    private String safeUsername()
    {
        try { return SecurityUtils.getUsername(); } catch (Exception e) { return "system"; }
    }

    /**
     * T5 拖拽调整：检查将排课移动到目标星期/节次窗口（周次保持自身不变）是否产生冲突。
     * 三类检测均复用 T2 修正后的口径：教室按占用记录+周次重叠判定，教师经开课表取 teacherId，
     * 学生按选课名单精确判定（定位查询仅扫本开课学生，避免全学期量级开销）。
     */
    @Override
    public List<ScheduleConflict> checkTargetSlotConflicts(Long scheduleId, Integer weekDay, Integer startPeriod, Integer endPeriod)
    {
        List<ScheduleConflict> conflicts = new ArrayList<>();
        TpmSchedule base = scheduleMapper.selectTpmScheduleByScheduleId(scheduleId);
        if (base == null)
        {
            throw new com.yu.common.exception.ServiceException("排课记录不存在: " + scheduleId);
        }
        validateSlotParams(weekDay, startPeriod, endPeriod);
        // 一次性解析开课信息（课程名/教师ID/学期ID），避免多次查库
        com.yu.tpm.domain.TpmCourseOffering offering = base.getOfferingId() != null
                ? courseOfferingMapper.selectTpmCourseOfferingByOfferingId(base.getOfferingId()) : null;
        String courseName = offering != null && offering.getCourseName() != null
                ? offering.getCourseName()
                : (base.getOfferingId() != null ? "课程ID " + base.getOfferingId() : "未知课程");
        Long teacherId = offering != null ? offering.getTeacherId() : null;
        Long semesterId = offering != null ? offering.getSemesterId() : null;

        // 教室占用冲突：目标教室同星期节次重叠且周次重叠的其他排课（排除自身）
        if (base.getClassroomId() != null)
        {
            List<TpmSchedule> roomOccupants = scheduleMapper.selectByClassroomAndTimeWithInfo(
                    base.getClassroomId(), weekDay, startPeriod, endPeriod);
            if (roomOccupants != null)
            {
                for (TpmSchedule other : roomOccupants)
                {
                    if (scheduleId.equals(other.getScheduleId())) { continue; }
                    if (!TimeSlotUtils.hasWeeksOverlap(base.getStartWeek(), base.getEndWeek(),
                            other.getStartWeek(), other.getEndWeek())) { continue; }
                    ScheduleConflict c = new ScheduleConflict();
                    c.setScheduleId1(scheduleId);
                    c.setScheduleId2(other.getScheduleId());
                    c.setConflictType(CLASSROOM_CONFLICT);
                    c.setCourseName1(courseName);
                    c.setCourseName2(other.getCourseName() != null ? other.getCourseName() : "该时段已排课程");
                    c.setClassroomName(other.getClassroomName() != null ? other.getClassroomName() : ("教室ID " + base.getClassroomId()));
                    c.setTimeDesc(formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()));
                    c.setMessage(String.format("教室冲突：%s 移至 %s 时，该教室同时段已有其他排课", courseName, formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek())));
                    conflicts.add(c);
                }
            }
        }

        // 教师占用冲突：经开课表取任课教师，查其同时段其他排课（排除自身）
        if (teacherId != null)
        {
            List<TpmSchedule> teacherOccupants = scheduleMapper.selectByTeacherAndTime(
                    teacherId, weekDay, startPeriod, endPeriod);
            if (teacherOccupants != null)
            {
                for (TpmSchedule other : teacherOccupants)
                {
                    if (scheduleId.equals(other.getScheduleId())) { continue; }
                    if (!TimeSlotUtils.hasWeeksOverlap(base.getStartWeek(), base.getEndWeek(),
                            other.getStartWeek(), other.getEndWeek())) { continue; }
                    ScheduleConflict c = new ScheduleConflict();
                    c.setScheduleId1(scheduleId);
                    c.setScheduleId2(other.getScheduleId());
                    c.setConflictType(TEACHER_CONFLICT);
                    c.setCourseName1(courseName);
                    c.setCourseName2(other.getCourseName() != null ? other.getCourseName() : "其他课程");
                    c.setClassroomName(other.getClassroomName());
                    c.setTimeDesc(formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()));
                    c.setMessage(String.format("教师冲突：%s 移至 %s 时，任课教师该时段已有《%s》",
                            courseName, formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()), c.getCourseName2()));
                    conflicts.add(c);
                }
            }
        }

        // 学生/班级冲突：本开课学生在本学期其他开课的窗口重叠课表槽（SQL 已含周次过滤，按学生对去重）
        if (semesterId != null)
        {
            List<StudentScheduleSlot> slots = scheduleMapper.selectStudentSlotsByOfferingInWindow(
                    base.getOfferingId(), semesterId, weekDay, startPeriod, endPeriod,
                    base.getStartWeek(), base.getEndWeek());
            Set<String> reported = new HashSet<>();
            if (slots != null)
            {
                for (StudentScheduleSlot s : slots)
                {
                    long s2 = s.getScheduleId() == null ? 0 : s.getScheduleId();
                    if (!reported.add(CLASS_CONFLICT + ":" + scheduleId + "-" + s2)) { continue; }
                    ScheduleConflict c = new ScheduleConflict();
                    c.setScheduleId1(scheduleId);
                    c.setScheduleId2(s.getScheduleId());
                    c.setConflictType(CLASS_CONFLICT);
                    c.setCourseName1(courseName);
                    c.setCourseName2(s.getCourseName() != null ? s.getCourseName() : "其他课程");
                    c.setClassroomName("学生[" + s.getStudentId() + "]");
                    c.setTimeDesc(formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()));
                    c.setMessage(String.format("班级冲突：选了《%s》的学生在 %s 已有《%s》",
                            courseName, formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()), c.getCourseName2()));
                    conflicts.add(c);
                }
            }
        }
        return conflicts;
    }

    /**
     * T5 拖拽调整：将排课移动到目标星期/节次窗口并落库（保持连堂跨度与周次不变）。
     * 非强制模式下存在冲突即拒绝，回传冲突明细供前端提示。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyDragAdjust(Long scheduleId, Integer weekDay, Integer startPeriod, Integer endPeriod, boolean force)
    {
        Map<String, Object> result = new HashMap<>();
        TpmSchedule base = scheduleMapper.selectTpmScheduleByScheduleId(scheduleId);
        if (base == null)
        {
            throw new com.yu.common.exception.ServiceException("排课记录不存在: " + scheduleId);
        }
        validateSlotParams(weekDay, startPeriod, endPeriod);

        List<ScheduleConflict> conflicts = force ? new ArrayList<>()
                : checkTargetSlotConflicts(scheduleId, weekDay, startPeriod, endPeriod);
        result.put("conflicts", conflicts);
        if (!force && !conflicts.isEmpty())
        {
            result.put("success", false);
            result.put("message", "目标时段存在 " + conflicts.size() + " 处冲突，未执行调整；可选择不保存或忽略冲突强制保存");
            return result;
        }

        TpmSchedule update = new TpmSchedule();
        update.setScheduleId(scheduleId);
        update.setWeekDay(weekDay);
        update.setStartPeriod(startPeriod);
        update.setEndPeriod(endPeriod);
        update.setUpdateBy(safeUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        scheduleMapper.updateTpmSchedule(update);
        log.info("T5 拖拽调整排课 {} 至 {}（force={}）", scheduleId, formatSlot(weekDay, startPeriod, endPeriod, base.getStartWeek(), base.getEndWeek()), force);

        result.put("success", true);
        result.put("schedule", scheduleMapper.selectTpmScheduleByScheduleId(scheduleId));
        result.put("message", force && !conflicts.isEmpty() ? "已忽略冲突强制保存" : "调整成功");
        return result;
    }

    /** 拖拽目标时段参数合法性校验 */
    private void validateSlotParams(Integer weekDay, Integer startPeriod, Integer endPeriod)
    {
        if (weekDay == null || weekDay < 1 || weekDay > 7)
        {
            throw new com.yu.common.exception.ServiceException("星期参数非法（1-7）: " + weekDay);
        }
        if (startPeriod == null || endPeriod == null || startPeriod < 1 || endPeriod < startPeriod)
        {
            throw new com.yu.common.exception.ServiceException("节次窗口非法: " + startPeriod + "-" + endPeriod);
        }
    }

    /** 生成时段描述文本 */
    private String formatSlot(Integer weekDay, Integer startPeriod, Integer endPeriod, Integer startWeek, Integer endWeek)
    {
        String[] weekDayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String dayName = (weekDay != null && weekDay >= 1 && weekDay <= 7) ? weekDayNames[weekDay] : ("星期" + weekDay);
        return String.format("%s 第%d-%d节 第%d-%d周", dayName,
                startPeriod != null ? startPeriod : 0, endPeriod != null ? endPeriod : 0,
                startWeek != null ? startWeek : 0, endWeek != null ? endWeek : 0);
    }
}
