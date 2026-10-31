package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.common.core.domain.AjaxResult;
import com.yu.common.exception.ServiceException;
import com.yu.common.monitor.BizMetrics;
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
 * <p>覆盖 T6 公平抽签（确定性随机种子、超/未满容量分支、候补排名）、候补递补（按空余名额递补 + DB 占名额）、
 * 选课/退课/改选的名额增减（T-3：DB 条件更新 occupySeat / releaseSeat）、
 * 选课冲突检测（时间冲突判定）、重复选课拦截与唯一键冲突转译（P-2）。</p>
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
    @Mock private BizMetrics bizMetrics;

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
        // T-4 后轮次状态机会在首次抽签后置为已结束，复现验证需每轮拿到“进行中”的新轮次快照
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound(), openRound());
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

    /** 构造一个“进行中”的新轮次对象（模拟每次从 DB 读到的独立快照，避免首次抽签的状态回写干扰下一轮） */
    private TpmSelectionRound openRound() {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setRoundStatus("1");
        return round;
    }

    @Test
    @DisplayName("抽签幂等：轮次已结束（抽签已完成）时拒绝重抽，不产生任何更新")
    void runLottery_closedRound_rejected() {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setRoundStatus("2"); // 已结束（首次抽签后置位）
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);

        Map<String, Object> r = service.runLottery(1L, 4242L);

        assertTrue(String.valueOf(r.get("message")).contains("不允许抽签"), "已结束轮次必须被状态机挡住：" + r.get("message"));
        assertEquals(null, r.get("successCount"), "拒绝重抽时不应返回统计结果");
        verify(enrollmentMapper, times(0)).selectTpmSelectionEnrollmentList(any());
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
        verify(enrollmentMapper, times(0)).batchUpdateLotteryResult(any(), any());
    }

    @Test
    @DisplayName("抽签幂等：查询只纳入未抽签（lottery_result=0）记录，已产生结果不参与重洗")
    void runLottery_queriesOnlyUndrawnRecords() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 20L))));
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(3);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        service.runLottery(1L, 4242L);

        org.mockito.ArgumentCaptor<TpmSelectionEnrollment> cap =
                org.mockito.ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(enrollmentMapper).selectTpmSelectionEnrollmentList(cap.capture());
        assertEquals("0", cap.getValue().getLotteryResult(), "抽签查询必须限定未抽签记录，否则会覆盖旧中签结果");
    }

    /** 从最近一次 runLottery 的批量 update 调用中捕获中签（lotteryResult=1）学生ID */
    private List<Long> lastUpdatedWinners() {
        org.mockito.ArgumentCaptor<List> cap = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(enrollmentMapper, atLeastOnce()).batchUpdateLotteryResult(cap.capture(), any());
        List<TpmSelectionEnrollment> all = new ArrayList<>();
        for (Object o : cap.getAllValues()) {
            @SuppressWarnings("unchecked")
            List<TpmSelectionEnrollment> part = (List<TpmSelectionEnrollment>) o;
            all.addAll(part);
        }
        // 取本批次（最后一次 runLottery 会 batch 更新 5 条）
        List<TpmSelectionEnrollment> batch = all.subList(Math.max(0, all.size() - 5), all.size());
        return batch.stream().filter(e -> "1".equals(e.getLotteryResult()))
                .map(TpmSelectionEnrollment::getStudentId).sorted().collect(Collectors.toList());
    }

    @Test
    @DisplayName("P-3：抽签落库按 500 行分块，不再逐行 update")
    void runLottery_batchesInChunks() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        List<TpmSelectionEnrollment> list = new ArrayList<>();
        for (long i = 1; i <= 1200; i++) {
            list.add(enroll(i, 1000L + i, 20L));
        }
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(list);
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(1200); // 未超容量
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        service.runLottery(1L, 4242L);

        org.mockito.ArgumentCaptor<List> cap = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(enrollmentMapper, times(3)).batchUpdateLotteryResult(cap.capture(), any());
        List<Integer> sizes = cap.getAllValues().stream().map(List::size).collect(Collectors.toList());
        assertEquals(Arrays.asList(500, 500, 200), sizes, "1200 行必须切成 500+500+200 三条语句");
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
    }

    @Test
    @DisplayName("P-3：批量落库携带三列完整语义（中签清排名、落选带候补排名）")
    void runLottery_batchCarriesAllColumns() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        List<TpmSelectionEnrollment> list = new ArrayList<>(Arrays.asList(
                enroll(1L, 101L, 20L), enroll(2L, 102L, 20L), enroll(3L, 103L, 20L),
                enroll(4L, 104L, 20L), enroll(5L, 105L, 20L)));
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(list);
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(2);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);

        service.runLottery(1L, 777L);

        org.mockito.ArgumentCaptor<List> cap = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(enrollmentMapper).batchUpdateLotteryResult(cap.capture(), any());
        @SuppressWarnings("unchecked")
        List<TpmSelectionEnrollment> sent = (List<TpmSelectionEnrollment>) cap.getValue();
        assertEquals(5, sent.size(), "一次抽签的 5 条记录必须都在同一批里（未超 500）");
        for (TpmSelectionEnrollment e : sent) {
            assertTrue(e.getLotteryResult() != null && e.getResultStatus() != null, "三列不得漏传：" + e.getEnrollId());
            if ("1".equals(e.getLotteryResult())) {
                assertEquals(null, e.getWaitlistRank(), "中签者必须清空候补排名");
            } else {
                assertTrue(e.getWaitlistRank() != null, "落选者必须有候补排名");
            }
        }
    }

    @Test
    @DisplayName("P-3：无待落库记录时不得调用批量接口（空列表会拼出非法 SQL）")
    void runLottery_noPendingRecord_skipsBatch() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 20L))));
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(null); // 开课被删/无容量

        service.runLottery(1L, 4242L);

        verify(enrollmentMapper, times(0)).batchUpdateLotteryResult(any(), any());
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
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
    @DisplayName("候选递补：按空余名额递补，每递补一名先占 DB 名额")
    void promoteWaitlist_fillsFreeSeats() {
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(3); // 空 2
        when(enrollmentMapper.selectWaitlistByOffering(20L)).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(11L, 201L, 20L), enroll(12L, 202L, 20L), enroll(13L, 203L, 20L))));
        when(offeringMapper.occupySeat(20L)).thenReturn(1); // 名额充裕
        when(enrollmentMapper.promoteFromWaitlist(anyLong(), any())).thenReturn(1); // 条件迁移命中

        Map<String, Object> r = service.promoteWaitlist(20L);

        assertEquals(2, r.get("promoted"), "仅有 2 个空位，递补前 2 名候补");
        verify(enrollmentMapper, times(2)).promoteFromWaitlist(anyLong(), any());
        verify(offeringMapper, times(2)).occupySeat(20L);
    }
    
    @Test
    @DisplayName("候选递补：名额被并发抢走时立即停止，不超额递补")
    void promoteWaitlist_seatTakenConcurrently_stops() {
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(3); // 内存视图空 2
        List<TpmSelectionEnrollment> waitlist = new ArrayList<>(Arrays.asList(
                enroll(11L, 201L, 20L), enroll(12L, 202L, 20L), enroll(13L, 203L, 20L)));
        when(enrollmentMapper.selectWaitlistByOffering(20L)).thenReturn(waitlist);
        when(offeringMapper.occupySeat(20L)).thenReturn(1, 0); // 第二个名额已被别人占走
        when(enrollmentMapper.promoteFromWaitlist(anyLong(), any())).thenReturn(1);

        Map<String, Object> r = service.promoteWaitlist(20L);

        assertEquals(1, r.get("promoted"), "名额抢不到就停，不能把候补拔到容量外");
        verify(enrollmentMapper, times(1)).promoteFromWaitlist(anyLong(), any());
    }

    @Test
    @DisplayName("候补递补并发：候补行已被另一路递补时归还名额并停止")
    void promoteWaitlist_lostRace_returnsSeat() {
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(enrollmentMapper.countAdmittedByOffering(20L)).thenReturn(4); // 空 1
        when(enrollmentMapper.selectWaitlistByOffering(20L)).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(11L, 201L, 20L))));
        when(offeringMapper.occupySeat(20L)).thenReturn(1);
        // 递补条件迁移失败：该行已被另一路退课触发的递补升为中签
        when(enrollmentMapper.promoteFromWaitlist(anyLong(), any())).thenReturn(0);

        Map<String, Object> r = service.promoteWaitlist(20L);

        assertEquals(0, r.get("promoted"), "没改到记录就不能算递补成功");
        verify(offeringMapper).releaseSeat(20L); // 刚占的名额必须归还
        verify(cacheManager, times(0)).clearStudentCache(anyLong(), anyLong());
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
        TpmCourseOffering off20 = new TpmCourseOffering();
        off20.setCourseId(200L); off20.setMaxStudents(50); off20.setEnrolledCount(0);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(10L)).thenReturn(off10);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        TpmCourseLibrary c100 = new TpmCourseLibrary(); c100.setCourseName("高等数学"); c100.setCredit(3.0);
        TpmCourseLibrary c200 = new TpmCourseLibrary(); c200.setCredit(2.0);
        when(libraryMapper.selectTpmCourseLibraryByCourseId(100L)).thenReturn(c100);
        when(libraryMapper.selectTpmCourseLibraryByCourseId(200L)).thenReturn(c200);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertEquals(1, w.size(), "学分未超(5<30)、容量未满，应仅 1 条时间冲突");
        assertEquals("TIME_CONFLICT", w.get(0).getConflictType());
        assertTrue(w.get(0).getMessage().contains("高等数学"));
    }

    @Test
    @DisplayName("冲突检测：容量已满报 COURSE_FULL（读 enrolled_count 真相列）")
    void checkConflicts_courseFull() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        TpmCourseOffering off20 = new TpmCourseOffering();
        off20.setCourseId(200L); off20.setMaxStudents(2); off20.setEnrolledCount(2);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertTrue(w.stream().anyMatch(x -> "COURSE_FULL".equals(x.getConflictType())));
    }

    @Test
    @DisplayName("冲突检测：落选/候补（result_status=2）不占名额，不能按报名行数判满")
    void checkConflicts_waitlistedNotCountedAsFull() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        // 真相列：在册 30 / 上限 60，还剩名额
        TpmCourseOffering off20 = new TpmCourseOffering();
        off20.setCourseId(200L); off20.setMaxStudents(60); off20.setEnrolledCount(30);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        // 报名行里另有 170 条落选/候补：旧口径（result_status != '3'）会数成 200 人而误判已满
        lenient().when(enrollmentMapper.selectCountByOffering(20L)).thenReturn(200);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertTrue(w.stream().noneMatch(x -> "COURSE_FULL".equals(x.getConflictType())),
                "在册 30 < 上限 60，落选行不得把容量算满");
        verify(enrollmentMapper, never()).selectCountByOffering(anyLong());
    }

    @Test
    @DisplayName("冲突检测：选课规则违规转成 RULE_VIOLATION 警告")
    void checkConflicts_ruleViolation() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        TpmCourseOffering off20 = new TpmCourseOffering();
        off20.setCourseId(200L); off20.setMaxStudents(50); off20.setEnrolledCount(0);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off20);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(Arrays.asList("未满足先修课程要求"));

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertTrue(w.stream().anyMatch(x -> "RULE_VIOLATION".equals(x.getConflictType())
                && x.getMessage().contains("先修")));
    }

    @Test
    @DisplayName("P-2：冲突预检对重复选课返回 DUPLICATE_COURSE，而不是自撞的时间冲突")
    void checkConflicts_duplicateCourse() {
        ReflectionTestUtils.setField(service, "defaultMaxCredits", 30.0);
        // 同一门课的排课当然与自己重叠：不先判重就会报成「第3-4节 与 第3-4节 冲突」
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>(Arrays.asList(sched(3, 3, 4, 1, 16))));
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L))
                .thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 20L))));

        List<ConflictWarning> w = service.checkSelectionConflicts(101L, 20L, 1L);

        assertEquals(1, w.size(), "重复选课只报一条，不再叠加时间/学分误报");
        assertEquals("DUPLICATE_COURSE", w.get(0).getConflictType());
        assertTrue(w.get(0).getMessage().contains("已选过该课程"));
    }

    // ==================== dropCourse ====================

    @Test
    @DisplayName("退课：中签者退课置状态3、释放 DB 名额并触发候补递补")
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
        // T-3 并发收口：状态迁移改为条件更新，只有 affected=1 才允许释放名额
        when(enrollmentMapper.dropIfEnrolled(eq(1L), any())).thenReturn(1);

        AjaxResult r = service.dropCourse(1L);

        assertEquals(200, r.get(AjaxResult.CODE_TAG));
        verify(enrollmentMapper).dropIfEnrolled(eq(1L), any());
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
        verify(offeringMapper).releaseSeat(20L);
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
        verify(enrollmentMapper, times(0)).dropIfEnrolled(anyLong(), any());
        verify(offeringMapper, times(0)).releaseSeat(anyLong());
    }

    @Test
    @DisplayName("退课并发：条件迁移被抢时（affected=0）不得重复释放名额")
    void dropCourse_lostRace_doesNotReleaseSeat() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);
        TpmSelectionRound round = new TpmSelectionRound(); round.setRoundStatus("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        // 另一路已把记录退掉：条件迁移返回 0
        when(enrollmentMapper.dropIfEnrolled(eq(1L), any())).thenReturn(0);

        AjaxResult r = service.dropCourse(1L);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        verify(offeringMapper, times(0)).releaseSeat(anyLong());
        verify(cacheManager, times(0)).clearStudentCache(anyLong(), anyLong());
    }

    // ==================== enrollWithValidation（T-3 名额） ====================

    /** 选课链路公共桩：轮次进行中、无已选课程、无排课、待选课程容量未满 */
    private void stubCleanEnrollPath(Long offeringId, int maxStudents) {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        TpmCourseOffering off = new TpmCourseOffering();
        off.setMaxStudents(maxStudents);
        off.setEnrolledCount(0);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(offeringId)).thenReturn(off);
        when(ruleService.validate(1L, 101L, offeringId)).thenReturn(new ArrayList<>());
    }

    @Test
    @DisplayName("T-3：选课先占 DB 名额再落记录，两步同属一个事务")
    void enroll_occupiesSeatBeforeInsert() {
        stubCleanEnrollPath(20L, 50);
        // 用共享调用序列记录两步的先后：InOrder 会“跳过早先的不匹配调用”，拦不住“先 insert 再占名额”
        List<String> calls = new ArrayList<>();
        doAnswer(inv -> { calls.add("occupy"); return 1; }).when(offeringMapper).occupySeat(20L);
        doAnswer(inv -> { calls.add("insert"); return 1; })
                .when(enrollmentMapper).insertTpmSelectionEnrollment(any());

        AjaxResult r = service.enrollWithValidation(101L, 20L, 1L, null);

        assertEquals(200, r.get(AjaxResult.CODE_TAG));
        assertEquals(Arrays.asList("occupy", "insert"), calls,
                "必须先占到名额再落报名记录，顺序反了会留下无名额的报名行");
        verify(cacheManager).clearStudentCache(101L, 1L);
    }

    @Test
    @DisplayName("T-3：名额占不到且课程存在时判定已满，写超卖埋点且不落记录")
    void enroll_full_rejected() {
        stubCleanEnrollPath(20L, 50);
        when(offeringMapper.occupySeat(20L)).thenReturn(0);

        AjaxResult r = service.enrollWithValidation(101L, 20L, 1L, null);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        assertTrue(String.valueOf(r.get(AjaxResult.MSG_TAG)).contains("课程已满"));
        verify(bizMetrics).recordOversell();
        verify(enrollmentMapper, times(0)).insertTpmSelectionEnrollment(any());
    }

    @Test
    @DisplayName("T-3：开课不存在/容量未配置时返回课程信息异常，不计超卖")
    void enroll_offeringMissing_notOversell() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(null);
        when(ruleService.validate(1L, 101L, 20L)).thenReturn(new ArrayList<>());
        when(offeringMapper.occupySeat(20L)).thenReturn(0);

        AjaxResult r = service.enrollWithValidation(101L, 20L, 1L, null);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        assertTrue(String.valueOf(r.get(AjaxResult.MSG_TAG)).contains("课程信息异常"));
        verify(bizMetrics, times(0)).recordOversell();
    }

    @Test
    @DisplayName("P-2：重复选同一门课在占名额前就被拦下，不消耗名额")
    void enroll_duplicateCourse_rejectedBeforeOccupy() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L))
                .thenReturn(new ArrayList<>(Arrays.asList(enroll(1L, 101L, 20L))));
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>(Arrays.asList(sched(3, 3, 4, 1, 16))));

        AjaxResult r = service.enrollWithValidation(101L, 20L, 1L, null);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        assertTrue(String.valueOf(r.get(AjaxResult.MSG_TAG)).contains("已选过该课程"),
                "重复选课要说真实原因，不能报成与自己时间冲突：" + r.get(AjaxResult.MSG_TAG));
        verify(offeringMapper, times(0)).occupySeat(anyLong());
        verify(enrollmentMapper, times(0)).insertTpmSelectionEnrollment(any());
    }

    @Test
    @DisplayName("P-2：并发重复提交被唯一键拦下时抛业务异常（让事务回滚、占的名额一起撤销）")
    void enroll_duplicateKey_throwsServiceException() {
        stubCleanEnrollPath(20L, 50);
        when(offeringMapper.occupySeat(20L)).thenReturn(1);
        when(enrollmentMapper.insertTpmSelectionEnrollment(any()))
                .thenThrow(new DuplicateKeyException(
                        "Duplicate entry '1-101-20-1' for key 'tpm_selection_enrollment.uk_enroll_active'"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.enrollWithValidation(101L, 20L, 1L, null));

        assertTrue(ex.getMessage().contains("请勿重复提交"), "必须是可读的业务提示，而不是 MySQL 原文：" + ex.getMessage());
        // 不能吞异常去返回 AjaxResult：返回值会让事务提交，刚占的名额就永久泄漏了
        verify(cacheManager, times(0)).clearStudentCache(anyLong(), anyLong());
    }

    // ==================== swapCourse（T-3 先占后放） ====================

    @Test
    @DisplayName("T-3：改选先占新名额、后释放旧名额，失败不丢原课")
    void swapCourse_occupiesNewThenReleasesOld() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setAllowDropAdjust("1"); // 退改选窗口开放，且未配置起止时间
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        TpmCourseOffering off21 = new TpmCourseOffering(); off21.setMaxStudents(50); off21.setEnrolledCount(1);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(21L)).thenReturn(off21);
        when(ruleService.validate(1L, 101L, 21L)).thenReturn(new ArrayList<>());
        when(offeringMapper.occupySeat(21L)).thenReturn(1);
        when(enrollmentMapper.swapIfEnrolled(eq(1L), eq(20L), eq(21L), any())).thenReturn(1);

        AjaxResult r = service.swapCourse(1L, 21L);

        assertEquals(200, r.get(AjaxResult.CODE_TAG));
        verify(enrollmentMapper).swapIfEnrolled(eq(1L), eq(20L), eq(21L), any());
        verify(enrollmentMapper, times(0)).updateTpmSelectionEnrollment(any());
        InOrder io = inOrder(offeringMapper);
        io.verify(offeringMapper).occupySeat(21L);
        io.verify(offeringMapper).releaseSeat(20L);
        verify(cacheManager).clearStudentCache(101L, 1L);
    }

    @Test
    @DisplayName("改选并发：条件迁移被抢时（affected=0）归还新名额且不释放旧名额")
    void swapCourse_lostRace_returnsNewSeatOnly() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setAllowDropAdjust("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        TpmCourseOffering off21 = new TpmCourseOffering(); off21.setMaxStudents(50); off21.setEnrolledCount(1);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(21L)).thenReturn(off21);
        when(ruleService.validate(1L, 101L, 21L)).thenReturn(new ArrayList<>());
        when(offeringMapper.occupySeat(21L)).thenReturn(1);
        // 记录已被另一路改走/退掉：旧开课栅栏不命中
        when(enrollmentMapper.swapIfEnrolled(eq(1L), eq(20L), eq(21L), any())).thenReturn(0);

        AjaxResult r = service.swapCourse(1L, 21L);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        verify(offeringMapper).releaseSeat(21L); // 刚占的新名额要归还
        verify(offeringMapper, times(0)).releaseSeat(20L); // 旧名额不得释放（记录还在旧课上）
        verify(cacheManager, times(0)).clearStudentCache(anyLong(), anyLong());
    }

    @Test
    @DisplayName("T-3：目标课程已满时改选失败，不释放原课名额")
    void swapCourse_targetFull_keepsOldSeat() {
        TpmSelectionEnrollment e = enroll(1L, 101L, 20L);
        when(enrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(1L)).thenReturn(e);
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setAllowDropAdjust("1");
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round);
        when(enrollmentMapper.selectByStudentAndRound(101L, 1L)).thenReturn(new ArrayList<>());
        when(scheduleMapper.selectTpmScheduleList(any())).thenReturn(new ArrayList<>());
        TpmCourseOffering off21 = new TpmCourseOffering(); off21.setMaxStudents(50); off21.setEnrolledCount(1);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(21L)).thenReturn(off21);
        when(ruleService.validate(1L, 101L, 21L)).thenReturn(new ArrayList<>());
        when(offeringMapper.occupySeat(21L)).thenReturn(0);

        AjaxResult r = service.swapCourse(1L, 21L);

        assertEquals(500, r.get(AjaxResult.CODE_TAG));
        assertTrue(String.valueOf(r.get(AjaxResult.MSG_TAG)).contains("目标课程已满"));
        assertEquals(20L, e.getCourseOfferingId(), "失败时报名记录不得改动");
        verify(offeringMapper, times(0)).releaseSeat(anyLong());
        verify(bizMetrics).recordOversell();
    }

    // ==================== 容量重算 ====================

    @Test
    @DisplayName("T-3：抽签落库后按报名表重算各开课已占名额")
    void runLottery_recountsEnrolledSeats() {
        when(roundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(openRound());
        when(enrollmentMapper.selectTpmSelectionEnrollmentList(any())).thenReturn(new ArrayList<>(Arrays.asList(
                enroll(1L, 101L, 20L), enroll(2L, 102L, 20L), enroll(3L, 103L, 21L))));
        TpmCourseOffering off = new TpmCourseOffering(); off.setMaxStudents(5);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(20L)).thenReturn(off);
        when(offeringMapper.selectTpmCourseOfferingByOfferingId(21L)).thenReturn(off);

        service.runLottery(1L, 999L);

        org.mockito.ArgumentCaptor<List> cap = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(offeringMapper).recountEnrolled(cap.capture());
        @SuppressWarnings("unchecked")
        List<Long> offeringIds = (List<Long>) cap.getValue();
        assertEquals(2, offeringIds.size(), "每门涉及的开课都要重算一次");
        assertTrue(offeringIds.containsAll(Arrays.asList(20L, 21L)));
    }
}
