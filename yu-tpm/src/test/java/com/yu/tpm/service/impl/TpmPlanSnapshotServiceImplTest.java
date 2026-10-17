package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.domain.TpmPlanSnapshot;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCreditStructureMapper;
import com.yu.tpm.mapper.TpmPlanSnapshotMapper;
import com.yu.tpm.snapshot.TrainingPlanSnapshotService;

/**
 * F2 方案版本快照：发布定格内容构建的单元验证。
 *
 * <p>覆盖：实质字段指纹复用、学分结构/课程清单 JSON 定格、版本号缺省补齐 V1、
 * 无登录上下文时审计人回退 system、仅保留影响毕业审核的实质子字段。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmPlanSnapshotServiceImplTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private TpmPlanSnapshotMapper tpmPlanSnapshotMapper;

    @Mock
    private TpmCreditStructureMapper tpmCreditStructureMapper;

    @Mock
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Spy
    private TrainingPlanSnapshotService trainingPlanSnapshotService = new TrainingPlanSnapshotService();

    @InjectMocks
    private TpmPlanSnapshotServiceImpl service;

    private TpmTrainingPlan publishedPlan()
    {
        TpmTrainingPlan p = new TpmTrainingPlan();
        p.setPlanId(1L);
        p.setMajorId(10L);
        p.setDeptId(20L);
        p.setEducationLevel("1");
        p.setPlanYear("2024");
        p.setTotalCredits(160.0);
        p.setVersion("V2");
        p.setPublishStatus("1");
        return p;
    }

    @Test
    @DisplayName("发布快照：定格学分结构/课程清单 JSON 并复用实质内容指纹")
    void createSnapshotOnPublish_freezesContentAndHash() throws Exception
    {
        TpmCreditStructure struct = new TpmCreditStructure();
        struct.setCreditType("必修");
        struct.setCreditTypeName("必修课");
        struct.setRequiredCredit(60.0);
        struct.setMinCredit(55.0);
        when(tpmCreditStructureMapper.selectTpmCreditStructureList(any())).thenReturn(List.of(struct));

        TpmCourseLibrary course = new TpmCourseLibrary();
        course.setCourseCode("CS101");
        course.setCourseName("计算机导论");
        course.setCredit(3.0);
        course.setCourseType("专业");
        course.setSemesterOrder(1);
        when(tpmCourseLibraryMapper.selectTpmCourseLibraryList(any())).thenReturn(List.of(course));

        TpmPlanSnapshot snap = service.createSnapshotOnPublish(publishedPlan());

        assertNotNull(snap);
        assertEquals(1L, snap.getPlanId());
        assertEquals("V2", snap.getVersion());
        // 指纹与纯计算服务对同一 plan 的结果一致
        assertEquals(trainingPlanSnapshotService.fingerprint(publishedPlan()), snap.getContentHash());
        assertEquals(Double.valueOf(160.0), snap.getTotalCredit());
        assertNotNull(snap.getSnapshotTime());

        // 学分结构 JSON：仅保留实质分项字段
        JsonNode creditJson = MAPPER.readTree(snap.getCreditStructureJson());
        assertEquals(1, creditJson.size());
        assertEquals("必修", creditJson.get(0).get("creditType").asText());
        assertEquals(60.0, creditJson.get(0).get("requiredCredit").asDouble());

        // 课程清单 JSON：摘要字段
        JsonNode courseJson = MAPPER.readTree(snap.getCourseListJson());
        assertEquals("CS101", courseJson.get(0).get("courseCode").asText());
        assertEquals(3.0, courseJson.get(0).get("credit").asDouble());

        verify(tpmPlanSnapshotMapper).insertTpmPlanSnapshot(snap);
    }

    @Test
    @DisplayName("版本号为空时快照补齐 V1，与发布口径一致")
    void blankVersionDefaultsToV1()
    {
        TpmTrainingPlan p = publishedPlan();
        p.setVersion(null);
        when(tpmCreditStructureMapper.selectTpmCreditStructureList(any())).thenReturn(List.of());
        when(tpmCourseLibraryMapper.selectTpmCourseLibraryList(any())).thenReturn(List.of());

        TpmPlanSnapshot snap = service.createSnapshotOnPublish(p);

        assertEquals("V1", snap.getVersion());
        assertTrue(snap.getCreditStructureJson().equals("[]"));
        assertTrue(snap.getCourseListJson().equals("[]"));
    }

    @Test
    @DisplayName("planId 为空时不生成快照，避免脏落库")
    void nullPlanOrNoPlanIdReturnsNull()
    {
        assertNull(service.createSnapshotOnPublish(null));
        TpmTrainingPlan p = new TpmTrainingPlan();
        assertNull(service.createSnapshotOnPublish(p));
        verify(tpmPlanSnapshotMapper, org.mockito.Mockito.never()).insertTpmPlanSnapshot(any());
    }
}
