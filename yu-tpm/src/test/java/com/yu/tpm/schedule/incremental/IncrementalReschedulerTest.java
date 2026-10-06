package com.yu.tpm.schedule.incremental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.AutoScheduleItem;
import com.yu.tpm.domain.dto.ForbiddenSlot;
import com.yu.tpm.domain.dto.SchedulableClassroom;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import com.yu.tpm.schedule.dsl.ScheduleConstraintPolicy;

/**
 * N4 增量重排引擎单元验证：锁定占用避让、容量失败、硬同校区、硬禁排、空输入。
 * 纯逻辑无 DB/Spring，直接断言放置结果。
 *
 * @author N4
 */
class IncrementalReschedulerTest
{
    private final IncrementalRescheduler engine = new IncrementalRescheduler();

    private SchedulableClassroom room(long id, int cap, Long campusId, String type)
    {
        SchedulableClassroom r = new SchedulableClassroom();
        r.setClassroomId(id);
        r.setClassroomName("R" + id);
        r.setCapacity(cap);
        r.setCampusId(campusId);
        r.setBuildingId(9L);
        r.setTypeName(type);
        return r;
    }

    private ScheduleCandidate target(long offeringId, Integer maxStudents, Long campusId, Long teacherId, Integer totalHours)
    {
        ScheduleCandidate c = new ScheduleCandidate();
        c.setOfferingId(offeringId);
        c.setCourseName("课程" + offeringId);
        c.setMaxStudents(maxStudents);
        c.setCampusId(campusId);
        c.setTeacherId(teacherId);
        c.setTotalHours(totalHours);
        c.setPracticeHours(0);
        return c;
    }

    private TpmSchedule locked(Long classroomId, Long teacherId, int day, int sp, int ep)
    {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(1000L + day * 100 + sp);
        s.setClassroomId(classroomId);
        s.setTeacherId(teacherId);
        s.setWeekDay(day);
        s.setStartPeriod(sp);
        s.setEndPeriod(ep);
        s.setStartWeek(1);
        s.setEndWeek(16);
        return s;
    }

    private ForbiddenSlot forbidden(long teacherId, int day, int sp, int ep)
    {
        ForbiddenSlot fs = new ForbiddenSlot();
        fs.setTeacherId(teacherId);
        fs.setWeekDay(day);
        fs.setStartPeriod(sp);
        fs.setEndPeriod(ep);
        return fs;
    }

    @Test
    @DisplayName("锁定占用避让：受影响开课绕开已占用时段放置")
    void placesAroundLockedOccupancy()
    {
        List<SchedulableClassroom> rooms = List.of(room(5L, 100, 1L, "普通"));
        // 锁定：教室5 周一第1节已被占用
        List<TpmSchedule> locked = List.of(locked(5L, null, 1, 1, 1));
        // 目标：无教师、周课时2 → 2次会话
        List<ScheduleCandidate> targets = List.of(target(1L, 50, 1L, null, 32));

        IncrementalRescheduler.Result r = engine.reschedule(locked, List.of(), targets, rooms,
                5, 4, 1, 16, 30, 2, ScheduleConstraintPolicy.defaults());

        assertEquals(1, r.getRescheduledOfferings());
        assertEquals(2, r.getItems().size());
        AutoScheduleItem first = r.getItems().get(0);
        assertEquals(1, first.getWeekDay());
        assertEquals(2, first.getStartPeriod(), "周一第1节被锁定，应顺延到第2节");
    }

    @Test
    @DisplayName("容量不足：无教室满足则判失败")
    void failsWhenNoRoomFits()
    {
        List<SchedulableClassroom> rooms = List.of(room(5L, 100, 1L, "普通"));
        List<ScheduleCandidate> targets = List.of(target(1L, 1000, 1L, null, 16));

        IncrementalRescheduler.Result r = engine.reschedule(List.of(), List.of(), targets, rooms,
                5, 4, 2, 16, 30, 2, ScheduleConstraintPolicy.defaults());

        assertEquals(0, r.getRescheduledOfferings());
        assertEquals(1, r.getFailedOfferings());
    }

    @Test
    @DisplayName("硬同校区开启时异校区教室不可排；关闭时仍可（软约束）")
    void hardSameCampusGate()
    {
        List<SchedulableClassroom> rooms = List.of(room(5L, 100, 2L, "普通")); // 教室在校区2
        List<ScheduleCandidate> targets = List.of(target(1L, 50, 1L, null, 16)); // 开课期望校区1

        IncrementalRescheduler.Result strict = engine.reschedule(List.of(), List.of(), targets, rooms,
                5, 4, 2, 16, 30, 2, ScheduleConstraintPolicy.defaults().setHardSameCampus(true));
        assertEquals(1, strict.getFailedOfferings(), "强制同校区但无同校区教室应失败");

        IncrementalRescheduler.Result loose = engine.reschedule(List.of(), List.of(), targets, rooms,
                5, 4, 2, 16, 30, 2, ScheduleConstraintPolicy.defaults().setHardSameCampus(false));
        assertEquals(1, loose.getRescheduledOfferings(), "软约束下仍可跨校区排入");
    }

    @Test
    @DisplayName("硬禁排开启时避开禁排日；关闭时可用该日")
    void hardForbiddenGate()
    {
        List<SchedulableClassroom> rooms = List.of(room(5L, 100, 1L, "普通"));
        List<ScheduleCandidate> targets = List.of(target(1L, 50, 1L, 9L, 16)); // 教师9，1次会话
        List<ForbiddenSlot> forbidden = List.of(forbidden(9L, 1, 1, 2));       // 禁排周一1-2节

        IncrementalRescheduler.Result withForbid = engine.reschedule(List.of(), forbidden, targets, rooms,
                2, 2, 1, 16, 30, 2, ScheduleConstraintPolicy.defaults().setHardForbidden(true));
        assertEquals(2, withForbid.getItems().get(0).getWeekDay(), "周一被禁排，应排到周二");

        IncrementalRescheduler.Result noForbid = engine.reschedule(List.of(), forbidden, targets, rooms,
                2, 2, 1, 16, 30, 2, ScheduleConstraintPolicy.defaults().setHardForbidden(false));
        assertEquals(1, noForbid.getItems().get(0).getWeekDay(), "忽略禁排时周一可排");
    }

    @Test
    @DisplayName("空受影响集合直接返回空结果")
    void emptyTargets()
    {
        IncrementalRescheduler.Result r = engine.reschedule(List.of(), List.of(), new ArrayList<>(),
                List.of(room(5L, 100, 1L, "普通")), 5, 4, 2, 16, 30, 2, ScheduleConstraintPolicy.defaults());
        assertEquals(0, r.getRescheduledOfferings());
        assertEquals(0, r.getTotalSessions());
        assertTrue(r.getFailReasons().isEmpty());
    }
}
