package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.common.exception.ServiceException;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.SchedulableClassroom;
import com.yu.tpm.domain.dto.ScheduleCandidate;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.schedule.incremental.IncrementalRescheduler;

/**
 * N4 增量重排 Service 装配单元验证：锁定/受影响拆分、dryRun 与落库路径、DSL 校验异常。
 * 重排引擎以真实实例（@Spy）参与，仅隔离 Mapper。
 *
 * @author N4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleOptimizationServiceImplIncrementalTest
{
    @Mock
    private TpmScheduleMapper scheduleMapper;

    @Mock
    private TpmCourseOfferingMapper courseOfferingMapper;

    @Spy
    private IncrementalRescheduler incrementalRescheduler = new IncrementalRescheduler();

    @InjectMocks
    private ScheduleOptimizationServiceImpl service;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(service, "cfgDaysPerWeek", 5);
        ReflectionTestUtils.setField(service, "cfgPeriodsPerDay", 4);
        ReflectionTestUtils.setField(service, "cfgPeriodsPerSession", 1);
        ReflectionTestUtils.setField(service, "cfgTotalWeeks", 16);
        ReflectionTestUtils.setField(service, "defaultCapacity", 30);
        ReflectionTestUtils.setField(service, "cfgDefaultWeeklyHours", 2);
    }

    private SchedulableClassroom room(long id, int cap, Long campus)
    {
        SchedulableClassroom r = new SchedulableClassroom();
        r.setClassroomId(id);
        r.setClassroomName("R" + id);
        r.setCapacity(cap);
        r.setCampusId(campus);
        r.setBuildingId(9L);
        r.setTypeName("普通");
        return r;
    }

    private ScheduleCandidate cand(long offeringId)
    {
        ScheduleCandidate c = new ScheduleCandidate();
        c.setOfferingId(offeringId);
        c.setCourseName("课程" + offeringId);
        c.setMaxStudents(50);
        c.setCampusId(1L);
        c.setTotalHours(32);
        c.setPracticeHours(0);
        return c;
    }

    private TpmSchedule sched(long id, long offeringId, long roomId, int day, int sp, int ep)
    {
        TpmSchedule s = new TpmSchedule();
        s.setScheduleId(id);
        s.setOfferingId(offeringId);
        s.setClassroomId(roomId);
        s.setWeekDay(day);
        s.setStartPeriod(sp);
        s.setEndPeriod(ep);
        s.setStartWeek(1);
        s.setEndWeek(16);
        return s;
    }

    @Test
    @DisplayName("dryRun：受影响开课旧排课被腾空后重排，不落库、不删除")
    void dryRunPreview()
    {
        when(courseOfferingMapper.selectCandidatesByOfferingIds(anyList()))
                .thenReturn(List.of(cand(1L)));
        when(scheduleMapper.selectSchedulableClassrooms()).thenReturn(List.of(room(5L, 100, 1L)));
        // offering2 锁定占用(周一1节)，offering1(受影响)旧排课(周一2节)应被排除出锁定
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(new ArrayList<>(List.of(
                sched(600L, 2L, 5L, 1, 1, 1),
                sched(500L, 1L, 5L, 1, 2, 2))));
        when(scheduleMapper.selectForbiddenSlotsBySemester(eq(1L))).thenReturn(List.of());

        var result = service.incrementalReschedule(1L, List.of(1L), true, null);

        assertEquals(1, result.get("rescheduledOfferings"));
        assertEquals(0, result.get("inserted"));
        assertNotNull(result.get("items"));
        verify(scheduleMapper, never()).insertTpmSchedule(any());
        verify(scheduleMapper, never()).deleteTpmScheduleByScheduleIds(any());
    }

    @Test
    @DisplayName("落库：先删受影响旧排课再写入新排课")
    void persistDeletesStaleAndInserts()
    {
        when(courseOfferingMapper.selectCandidatesByOfferingIds(anyList()))
                .thenReturn(List.of(cand(1L)));
        when(scheduleMapper.selectSchedulableClassrooms()).thenReturn(List.of(room(5L, 100, 1L)));
        when(scheduleMapper.selectSchedulesBySemester(eq(1L))).thenReturn(new ArrayList<>(List.of(
                sched(600L, 2L, 5L, 1, 1, 1),
                sched(500L, 1L, 5L, 1, 2, 2))));
        when(scheduleMapper.selectForbiddenSlotsBySemester(eq(1L))).thenReturn(List.of());
        when(scheduleMapper.insertTpmSchedule(any(TpmSchedule.class))).thenReturn(1);

        var result = service.incrementalReschedule(1L, List.of(1L), false, null);

        verify(scheduleMapper).deleteTpmScheduleByScheduleIds(argThat(a -> a != null && a.length == 1 && a[0] == 500L));
        int sessions = (Integer) result.get("totalSessions");
        verify(scheduleMapper, times(sessions)).insertTpmSchedule(any(TpmSchedule.class));
        assertEquals(sessions, result.get("inserted"));
    }

    @Test
    @DisplayName("非法 DSL 抛出 ServiceException")
    void invalidDslRejected()
    {
        assertThrows(ServiceException.class,
                () -> service.incrementalReschedule(1L, List.of(1L), true, "bad.key=1"));
    }

    @Test
    @DisplayName("空开课ID抛出 ServiceException")
    void emptyOfferingIdsRejected()
    {
        assertThrows(ServiceException.class,
                () -> service.incrementalReschedule(1L, List.of(), true, null));
    }

    @Test
    @DisplayName("受影响集合无可重排开课：安全返回零")
    void noCandidatesSafe()
    {
        when(courseOfferingMapper.selectCandidatesByOfferingIds(anyList())).thenReturn(List.of());
        var result = service.incrementalReschedule(1L, List.of(99L), true, null);
        assertEquals(0, result.get("rescheduledOfferings"));
        verify(scheduleMapper, never()).selectSchedulableClassrooms();
    }
}
