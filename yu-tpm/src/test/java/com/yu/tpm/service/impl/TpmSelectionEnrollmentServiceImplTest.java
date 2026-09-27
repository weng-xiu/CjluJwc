package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.common.core.domain.AjaxResult;
import com.yu.framework.cache.SelectionCacheManager;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.tpm.service.ITpmSelectionRuleService;

/**
 * 选课核心算法单元测试（Q1 回归防护）。
 *
 * <p>覆盖 T6 公平抽签（确定性随机种子、超/未满容量分支、候补排名）、候补递补（按空余名额递补 + Redis 容量回写）、
 * 选课冲突检测（时间冲突判定）、退课（状态流转 + 容量回补 + 中签者退课触发自动递补）。</p>
 */
@ExtendWith(MockitoExtension.class)
class TpmSelectionEnrollmentServiceImplTest {

    @Mock private TpmSelectionEnrollmentMapper enrollmentMapper;
    @Mock private TpmScheduleMapper scheduleMapper;
    @Mock private TpmCourseOfferingMapper offeringMapper;
    @Mock private TpmCourseLibraryMapper libraryMapper;
    @Mock private TpmSelectionRoundMapper roundMapper;
    @Mock private BrmTeacherMapper brmTeacherMapper;
    @Mock private SelectionCacheManager cacheManager;
    @Mock private ITpmSelectionRuleService ruleService;

    @InjectMocks
    private TpmSelectionEnrollmentServiceImpl service;

    private TpmSelectionEnrollment enroll(Long id, Long studentId, Long offeringId) {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(id);
        e.setStudentId(studentId);
        e.setCourseOfferingId(offeringId);
        e.setRoundId(1L);
        e.setResultStatus("1");
        e.setLotteryResult("0");
        return e;
    }

    private TpmSchedule sched(int day, int sp, int ep, int sw, int ew) {
        TpmSchedule s = new TpmSchedule();
        s.setWeekDay(day); s.setStartPeriod(sp); s.setEndPeriod(ep);
        s.setStartWeek(sw); s.setEndWeek(ew);
        return s;
    }

    // ==================== runLottery ====================

    @Test
    @DisplayName("抽签：未超容量全部中签，无落选")
    void runLottery_underCapacity_allAdmitted() {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L); round.setRoundStatus("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 20L), enroll(2L, 102L, 20L))));
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(3);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        Map<String, Object> r = service.runLottery(1L, 999L);

        assertEquals(2, r.get("successCount"));
        assertEquals(0, r.get("failCount"));
        assertEquals(0, r.get("lotteryCourses"), "未超容量不计入抽签课程数");
    }

    @Test
    @DisplayName("抽签：超容量按容量截取，落选者候补排名连续")
    void runLottery_overCapacity_waitlistRanked() {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L); round.setRoundStatus("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        List<TpmSelectionEnrollment> list = new ArrayList<>(Arrays.asList(
                enroll(1L, 101L, 20L), enroll(2L, 102L, 20L), enroll(3L, 103L, 20L),
                enroll(4L, 104L, 20L), enroll(5L, 105L, 20L)));
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(list);
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(2);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        Map<String, Object> r = service.runLottery(1L, 777L);

        assertEquals(2, r.get("successCount"));
        assertEquals(3, r.get("failCount"));
        List<Integer> ranks = list.stream().filter(e -> "2".equals(e.getLotteryResult()))
                .map(TpmSelectionEnrollment::getWaitlistRank).sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), ranks, "落选候补排名应为连续的 1,2,3");
        assertEquals(1, r.get("lotteryCourses"));
    }

    @Test
    @DisplayName("抽签：相同随机种子结果可复现（审计要求）")
    void runLottery_sameSeed_reproducible() {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L); round.setRoundStatus("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(2);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        // 第一次
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(1L, 101L, 20L), enroll(2L, 102L, 20L), enroll(3L, 103L, 20L),
                enroll(4L, 104L, 20L), enroll(5L, 105L, 20L))));
        service.runLottery(1L, 4242L);
        List<Long> winners1 = lastUpdatedWinners();

        // 第二次（同种子、同名单）
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(1L, 101L, 20L), enroll(2L, 102L, 20L), enroll(3L, 103L, 20L),
                enroll(4L, 104L, 20L), enroll(5L, 105L, 20L))));
        service.runLottery(1L, 4242L);
        List<Long> winners2 = lastUpdatedWinners();

        assertEquals(winners1, winners2, "同种子同名单的中签集合必须一致");
    }

    /** 从最近一次 runLottery 的 update 调用中捕获中签（lotteryResult=1）学生ID */
    private List<Long> lastUpdatedWinners() {
        org.mockito.ArgumentCaptor<TpmSelectionEnrollment> cap = org.mockito.ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(enrollmentMapper, atLeastOnce()).updateTpmSelectionEnrollment(cap.capture());
        List<TpmSelectionEnrollment> all = cap.getAllValues();
        // 取本批次（最后一次 runLottery 会 update 5 条）
        List<TpmSelectionEnrollment> batch = all.subList(Math.max(0, all.size() - 5), all.size());
        return batch.stream().filter(e -> "1".equals(e.getLotteryResult()))
                .map(TpmSelectionEnrollment::getStudentId).sorted().collect(Collectors.toList());
    }

    @Test
    @DisplayName("抽签：轮次不存在直接返回")
    void runLottery_roundNotFound() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(9L)).thenReturn(null);
        Map<String, Object> r = service.runLottery(9L, 1L);
        assertEquals("轮次不存在", r.get("message"));
    }

    // ==================== promoteWaitlist ====================

    @Test
    @DisplayName("候补递补：按空余名额递补并回写 Redis 剩余容量")
    void promoteWaitlist_fillsFreeSeats() {
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(3); // 空 2
        when(enrollmentMapper.selectWaitlistByOffering(20L)).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(11L, 201L, 20L), enroll(12L, 202L, 20L), enroll(13L, 203L, 20L))));

        Map<String, Object> r = service.promoteWaitlist(20L);

        assertEquals(2, r.get("promoted"), "仅有 2 个空位，递补前 2 名候补");
        verify(enrollmentMapper, times(2)).promoteFromWaitlist(anyLong(), any());
        verify(cacheManager, times(2)).decrementCapacity(20L);
        verify(cacheManager).setCapacity(20L, 0); // 5-(3+2)
    }

    @Test
    @DisplayName("候补递补：无空余容量时不递补")
    void promoteWaitlist_noFreeSeat() {
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(3);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(3);

        Map<String, Object> r = service.promoteWaitlist(20L);

        assertEquals(0, r.get("promoted"));
        verify(enrollmentMapper, times(0)).promoteFromWaitlist(anyLong(), any());
    }

    // ==================== checkSelectionConflicts ====================

    @Test
    @DisplayName("冲突检测：与已选课程时间重叠报 TIME_CONFLICT 且不重复报")
    void checkConflicts_timeConflictReportedOnce() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        // 待选开课20：周一1-2
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>(Arrays.asList(sched(1, 1, 2, 1, 16))));
        // 学生已选开课10
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 10L))));
        // 开课10排课：周一2-3（与1-2在第2节交叉）。必须带 offeringId 供 groupingBy 分组
        TpmSchedule enrolledSched = sched(1, 2, 3, 1, 16);
        enrolledSched.setOfferingId(10L);
        when(scheduleMapper.selectByOfferingIds(any())).thenReturn(new ArrayList<>(Arrays.asList(enrolledSched)));
        TpmCourseOffering off10 = new TpmCourseOffering(); off10.setCourseId(100L); off10.setMaxStudents(50);
        TpmCourseOffering off20 = new TpmCourseOffering(); off20.setCourseId(200L); off20.setMaxStudents(50);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(10L)).thenReturn(off10);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        TpmCourseLibrary c100 = new TpmCourseLibrary(); c100.setCourseName("高等数学"); c100.setCredit(3.0);
        TpmCourseLibrary c200 = new TpmCourseLibrary(); c200.setCredit(2.0);
        when(libraryMapper.selectTpmCourseLibraryByCourseId(100L)).thenReturn(c100);
        when(libraryMapper.selectTpmCourseLibraryByCourseId(200L)).thenReturn(c200);
        when(enrollmentMapper.selectCountByOffering(20L)).thenReturn(0);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertEquals(1, w.size(), "学分未超(5<30)、容量未满，应仅 1 条时间冲突");
        assertEquals("TIME_CONFLICT", w.get(0).getConflictType());
        assertTrue(w.get(0).getMessage().contains("高等数学"));
    }

    @Test
    @DisplayName("冲突检测：容量已满报 COURSE_FULL")
    void checkConflicts_courseFull() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        TpmCourseOffering off20 = new TpmCourseOffering(); off20.setCourseId(200L); off20.setMaxStudents(2);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        when(enrollmentMapper.selectCountByOffering(20L)).thenReturn(2); // 已满
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertTrue(w.stream().anyMatch(x -> "COURSE_FULL".equals(x.getConflictType())));
    }

    @Test
    @DisplayName("冲突检测：选课规则违规转成 RULE_VIOLATION 警告")
    void checkConflicts_ruleViolation() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        TpmCourseOffering off20 = new TpmCourseOffering(); off20.setCourseId(200L); off20.setMaxStudents(50);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        when(enrollmentMapper.selectCountByOffering(20L)).thenReturn(0);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(Arrays.asList("未满足先修课程要求"));

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertTrue(w.stream().anyMatch(x -> "RULE_VIOLATION".equals(x.getConflictType())
                && x.getMessage().contains("先修")));
    }

    // ==================== dropCourse ====================

    @Test
    @DisplayName("退课：中签者退课置状态3、回补容量并触发候补递补")
    void dropCourse_admittedTriggersPromote() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        e.setLotteryResult("1"); // 中签
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);
        TpmSelectionRound round = new TpmSelectionRound(); round.setRoundStatus("2"); // 已结束
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        // 递补内部：容量已满（中签5=上限5）则查询后判定无空位，用于验证退课确实触发递补查询
        TpmCourseOffering off20 = new TpmCourseOffering(); off20.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(5);

        AjaxResult r = service.dropCourse(1L);

        assertEquals(200, r.get(AjaxResult.CODE_TAG));
        assertEquals("3", e.getResultStatus());
        verify(cacheManager).incrementCapacity(20L);
        verify(cacheManager).clearStudentCache(101L, 1L);
        // 中签者退课应触发候补递补查询
        verify(enrollmentMapper).countAdmittedByOffering(eq(20L));
    }

    @Test
    @DisplayName("退课：非选中状态拒绝")
    void dropCourse_invalidStatus() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        e.setResultStatus("2"); // 未选中
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);

        AjaxResult r = service.dropCourse(1L);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
    }
}
