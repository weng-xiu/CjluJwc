package com.yu.portal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.portal.mapper.PortalAiMapper;
import com.yu.system.service.ISysConfigService;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 门户 AI 服务层单元测试。
 *
 * <p>覆盖选课推荐的「在册口径」：落选/候补（result_status='2'）既不进已选清单，
 * 也不占门数与学分余量——否则学生抽签落选后打开门户会被告知“已达门数上限”。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalAiServiceImplTest {

    @Mock private PortalAiMapper portalAiMapper;
    @Mock private ITpmSelectionRoundService tpmSelectionRoundService;
    @Mock private ITpmCourseOfferingService tpmCourseOfferingService;
    @Mock private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;
    @Mock private ISysConfigService configService;

    @InjectMocks
    private PortalAiServiceImpl service;

    private TpmSelectionRound round;
    private TpmCourseOffering admitted;
    private TpmCourseOffering waitlisted;

    @BeforeEach
    void setUp() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("studentId", 101L);
        profile.put("majorId", 5L);
        when(portalAiMapper.selectStudentProfile(101L)).thenReturn(profile);
        when(portalAiMapper.selectPublishedPlan(anyLong())).thenReturn(null);
        when(portalAiMapper.selectCourseInfoByIds(anyList())).thenReturn(new ArrayList<>());
        when(portalAiMapper.selectStudentEarnedByCategory(anyLong())).thenReturn(new ArrayList<>());
        when(portalAiMapper.selectCourseEvalStat(anyList())).thenReturn(new ArrayList<>());
        when(portalAiMapper.selectEvalGlobalAvg()).thenReturn(null);
        when(portalAiMapper.selectStudentSemesterProgress(anyLong())).thenReturn(null);

        round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setSemesterId(99L);
        round.setRoundName("2026 秋季第一轮");
        round.setRoundStatus("1");
        round.setMaxCoursesPerStudent(2);
        when(tpmSelectionRoundService.selectTpmSelectionRoundList(any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(round)));

        // 一门在册（result_status='1'），一门落选（result_status='2'）
        admitted = new TpmCourseOffering();
        admitted.setOfferingId(20L);
        admitted.setCourseId(200L);
        admitted.setCourseName("高等数学");
        admitted.setCredit(2.0d);
        admitted.setMaxStudents(60);
        admitted.setEnrolledCount(30);

        waitlisted = new TpmCourseOffering();
        waitlisted.setOfferingId(30L);
        waitlisted.setCourseId(300L);
        waitlisted.setCourseName("大学物理");
        waitlisted.setCredit(3.0d);
        waitlisted.setMaxStudents(60);
        waitlisted.setEnrolledCount(60);

        when(tpmCourseOfferingService.selectTpmCourseOfferingListForPortal(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(admitted, waitlisted)));
        when(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(any()))
                .thenReturn(new ArrayList<>(Arrays.asList(
                        enrollment(1L, 20L, "1"),
                        enrollment(2L, 30L, "2"))));
        when(tpmSelectionEnrollmentService.checkSelectionConflicts(anyLong(), anyLong(), anyLong()))
                .thenReturn(new ArrayList<>());
    }

    private TpmSelectionEnrollment enrollment(long enrollId, long offeringId, String resultStatus) {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(enrollId);
        e.setStudentId(101L);
        e.setRoundId(1L);
        e.setCourseOfferingId(offeringId);
        e.setResultStatus(resultStatus);
        e.setLotteryResult("1");
        return e;
    }

    @Test
    @DisplayName("选课推荐：落选/候补不计入已选清单，也不占门数与学分余量")
    @SuppressWarnings("unchecked")
    void recommendCourses_waitlistedNotCountedAsSelected() {
        Map<String, Object> r = service.recommendCourses(101L);

        List<Map<String, Object>> selected = (List<Map<String, Object>>) r.get("selected");
        assertNotNull(selected, "已选清单不得为空");
        assertEquals(1, selected.size(), "落选（result_status='2'）不得进入已选清单，实际条目：" + selected);
        assertEquals(20L, selected.get(0).get("offeringId"), "已选清单只应含在册那门");

        Map<String, Object> quota = (Map<String, Object>) r.get("quota");
        assertEquals(1, quota.get("selectedCourses"), "已选门数只数在册");
        assertEquals(1, quota.get("remainingCourses"), "上限 2 门 - 在册 1 门 = 余 1 门（落选不扣）");
        assertEquals(2.0d, ((Number) quota.get("selectedCredit")).doubleValue(), 1e-9,
                "已选学分只算在册那门（2 学分），落选的 3 学分不得计入");

        List<Map<String, Object>> items = (List<Map<String, Object>>) r.get("items");
        assertNotNull(items, "落选课程应重新进入候选，而不是被判为“已在清单中”");
        boolean containsWaitlisted = items.stream()
                .anyMatch(it -> Long.valueOf(30L).equals(it.get("offeringId")));
        assertTrue(containsWaitlisted, "落选的课程应重新成为可推荐候选项");
    }
}
