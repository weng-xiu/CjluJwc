package com.yu.tpm.schedule.incremental;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.AutoScheduleItem;
import com.yu.tpm.domain.dto.ForbiddenSlot;
import com.yu.tpm.domain.dto.SchedulableClassroom;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import com.yu.tpm.schedule.dsl.ScheduleConstraintPolicy;

/**
 * N4 增量重排引擎（纯逻辑，无 DB/Spring 依赖，便于单测）。
 *
 * <p>与全量自动排课不同：仅对"受影响开课"重新求解放置，其余既有排课作为不可变占用锁定。
 * 典型场景——拖拽/删除某条排课后，只需为被腾空的开课重排，避免整学期全局重算的高开销。</p>
 *
 * <ol>
 *   <li>以 {@code lockedSchedules}（除受影响开课外的全部既有排课）+ 禁排（按策略）预热占用网格；</li>
 *   <li>仅遍历 {@code targets}，沿用"约束贪心 + 占用网格"择优放置，权重由 {@link ScheduleConstraintPolicy} 驱动；</li>
 *   <li>输出新排入条目与未排入原因，供上层决定是否落库。</li>
 * </ol>
 *
 * @author N4
 */
@Component
public class IncrementalRescheduler
{
    /** 重排结果 */
    public static class Result
    {
        private final List<AutoScheduleItem> items = new ArrayList<>();
        private final List<String> failReasons = new ArrayList<>();
        private int rescheduledOfferings;
        private int failedOfferings;

        public List<AutoScheduleItem> getItems() { return items; }
        public List<String> getFailReasons() { return failReasons; }
        public int getRescheduledOfferings() { return rescheduledOfferings; }
        public int getFailedOfferings() { return failedOfferings; }
        public int getTotalSessions() { return items.size(); }
    }

    /**
     * 对受影响开课执行增量重排。
     *
     * @param lockedSchedules  需锁定保留的既有排课（不含受影响开课，其时段视为已占用）
     * @param forbiddenSlots   教师禁排时间片（策略开启硬约束时生效）
     * @param targets          待重排的开课候选
     * @param rooms            可排课教室池
     * @param days             每周排课天数
     * @param ppd              每天最大节次
     * @param pps              每次连堂节数
     * @param weeks            学期总周数
     * @param defaultCapacity  无容量数据时的默认需求容量
     * @param defaultWeeklyHours 无学时数据时的默认周课时
     * @param policy           约束策略
     */
    public Result reschedule(List<TpmSchedule> lockedSchedules, List<ForbiddenSlot> forbiddenSlots,
                             List<ScheduleCandidate> targets, List<SchedulableClassroom> rooms,
                             int days, int ppd, int pps, int weeks,
                             int defaultCapacity, int defaultWeeklyHours,
                             ScheduleConstraintPolicy policy)
    {
        Result result = new Result();
        if (targets == null || targets.isEmpty())
        {
            return result;
        }
        ScheduleConstraintPolicy p = policy != null ? policy : ScheduleConstraintPolicy.defaults();

        Map<Long, boolean[][]> roomGrid = new HashMap<>();
        Map<Long, boolean[][]> teacherGrid = new HashMap<>();

        // 1) 锁定既有排课（视为不可动的占用）
        if (lockedSchedules != null)
        {
            for (TpmSchedule s : lockedSchedules)
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

        // 2) 禁排硬约束（策略关闭时不屏蔽，与既有行为一致）
        if (p.isHardForbidden() && forbiddenSlots != null)
        {
            for (ForbiddenSlot fs : forbiddenSlots)
            {
                if (fs.getTeacherId() == null || fs.getWeekDay() == null
                        || fs.getStartPeriod() == null || fs.getEndPeriod() == null) { continue; }
                int day = fs.getWeekDay();
                if (day < 1 || day > days) { continue; }
                int sp = Math.max(1, fs.getStartPeriod());
                int ep = Math.min(ppd, fs.getEndPeriod());
                markOccupied(teacherGrid.computeIfAbsent(fs.getTeacherId(), k -> newGrid(days, ppd)), day, sp, ep);
            }
        }

        Map<Long, int[]> teacherDayLoad = new HashMap<>();

        // 3) 仅为受影响开课放置
        for (ScheduleCandidate c : targets)
        {
            int reqCap = (c.getMaxStudents() != null && c.getMaxStudents() > 0) ? c.getMaxStudents() : defaultCapacity;
            int weeklyHours = deriveWeeklyHours(c, weeks, defaultWeeklyHours);
            int sessions = computeSessions(weeklyHours, pps, days);
            boolean needsLab = c.getPracticeHours() != null && c.getPracticeHours() > 0;

            Set<Integer> usedDays = new HashSet<>();
            Map<Integer, Integer> daySessionCount = new HashMap<>();
            Long primaryBuilding = null;
            int placed = 0;

            for (int sIdx = 0; sIdx < sessions; sIdx++)
            {
                List<Integer> dayOrder = orderDays(days, usedDays, c.getTeacherId(), teacherDayLoad, p.isDailyBalance());
                AutoScheduleItem item = null;
                outer:
                for (Integer day : dayOrder)
                {
                    if (p.getHardMaxSessionsPerDay() > 0
                            && daySessionCount.getOrDefault(day, 0) >= p.getHardMaxSessionsPerDay())
                    {
                        continue;
                    }
                    for (int start = 1; start + pps - 1 <= ppd; start++)
                    {
                        int end = start + pps - 1;
                        if (c.getTeacherId() != null && !blockFree(teacherGrid.get(c.getTeacherId()), day, start, end))
                        {
                            continue;
                        }
                        SchedulableClassroom best = chooseClassroom(rooms, roomGrid, day, start, end,
                                reqCap, needsLab, c.getCampusId(), primaryBuilding, p);
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
                daySessionCount.merge(item.getWeekDay(), 1, Integer::sum);
                if (primaryBuilding == null)
                {
                    SchedulableClassroom r = findRoomById(rooms, item.getClassroomId());
                    if (r != null) { primaryBuilding = r.getBuildingId(); }
                }
                result.getItems().add(item);
                placed++;
            }

            String label = c.getCourseName() != null ? c.getCourseName() : ("ID" + c.getOfferingId());
            if (placed == 0)
            {
                result.failedOfferings++;
                result.getFailReasons().add(String.format("开课[%s]（需容量%d，%s）：剩余时间片/教室不足，无法增量排入",
                        label, reqCap, needsLab ? "实践类" : "理论类"));
            }
            else
            {
                result.rescheduledOfferings++;
                if (placed < sessions)
                {
                    result.getFailReasons().add(String.format("开课[%s]：计划%d次/周，增量重排仅安置%d次（其余时段已被锁定占用）",
                            label, sessions, placed));
                }
            }
        }
        return result;
    }

    // ================= 放置辅助（与全量引擎同口径，改由策略驱动） =================

    private boolean[][] newGrid(int days, int ppd)
    {
        return new boolean[days + 1][ppd + 1];
    }

    private void markOccupied(boolean[][] grid, int day, int start, int end)
    {
        for (int pos = start; pos <= end; pos++)
        {
            if (day >= 0 && day < grid.length && pos >= 1 && pos < grid[day].length)
            {
                grid[day][pos] = true;
            }
        }
    }

    private boolean blockFree(boolean[][] grid, int day, int start, int end)
    {
        if (grid == null) { return true; }
        for (int pos = start; pos <= end; pos++)
        {
            if (day >= 0 && day < grid.length && pos >= 1 && pos < grid[day].length && grid[day][pos])
            {
                return false;
            }
        }
        return true;
    }

    private int deriveWeeklyHours(ScheduleCandidate c, int weeks, int defaultWeeklyHours)
    {
        int wh;
        if (c.getTotalHours() != null && c.getTotalHours() > 0 && weeks > 0)
        {
            wh = (int) Math.round(c.getTotalHours() / (double) weeks);
        }
        else
        {
            wh = defaultWeeklyHours;
        }
        return wh < 1 ? 1 : wh;
    }

    private int computeSessions(int weeklyHours, int pps, int days)
    {
        int sessions = (int) Math.ceil(weeklyHours / (double) pps);
        if (sessions < 1) { sessions = 1; }
        if (sessions > days) { sessions = days; }
        return sessions;
    }

    private List<Integer> orderDays(int days, Set<Integer> usedDays, Long teacherId,
                                    Map<Long, int[]> teacherDayLoad, boolean dailyBalance)
    {
        List<Integer> order = new ArrayList<>();
        for (int d = 1; d <= days; d++) { order.add(d); }
        final int[] load = teacherId != null ? teacherDayLoad.get(teacherId) : null;
        order.sort((a, b) -> {
            int ua = usedDays.contains(a) ? 1 : 0;
            int ub = usedDays.contains(b) ? 1 : 0;
            if (ua != ub) { return ua - ub; }
            if (dailyBalance)
            {
                int la = load != null ? load[a] : 0;
                int lb = load != null ? load[b] : 0;
                if (la != lb) { return la - lb; }
            }
            return a - b;
        });
        return order;
    }

    private SchedulableClassroom chooseClassroom(List<SchedulableClassroom> rooms, Map<Long, boolean[][]> roomGrid,
                                                 int day, int start, int end, int reqCap, boolean needsLab,
                                                 Long campusId, Long primaryBuilding, ScheduleConstraintPolicy p)
    {
        SchedulableClassroom best = null;
        int bestScore = Integer.MIN_VALUE;
        if (rooms == null) { return null; }
        for (SchedulableClassroom r : rooms)
        {
            if (r.getCapacity() == null || r.getCapacity() < reqCap) { continue; }
            if (!blockFree(roomGrid.get(r.getClassroomId()), day, start, end)) { continue; }
            boolean sameCampus = campusId != null && campusId.equals(r.getCampusId());
            // 硬约束：强制同校区时，异校区教室直接不可行
            if (p.isHardSameCampus() && campusId != null && !sameCampus) { continue; }
            boolean lab = isLabRoom(r.getTypeName());
            int score = p.scoreRoom(needsLab, lab, sameCampus,
                    primaryBuilding != null && primaryBuilding.equals(r.getBuildingId()),
                    r.getCapacity(), reqCap);
            if (score > bestScore)
            {
                bestScore = score;
                best = r;
            }
        }
        return best;
    }

    private boolean isLabRoom(String typeName)
    {
        if (typeName == null) { return false; }
        return typeName.contains("实验") || typeName.contains("机房") || typeName.contains("实践")
                || typeName.contains("实训") || typeName.contains("计算");
    }

    private SchedulableClassroom findRoomById(List<SchedulableClassroom> rooms, Long id)
    {
        if (id == null || rooms == null) { return null; }
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
}
