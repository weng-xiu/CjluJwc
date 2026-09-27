package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.mapper.TpmSelectionRuleMapper;

/**
 * 选课规则引擎单元测试（Q1 核心算法回归防护第二批）。
 *
 * <p>覆盖 TpmSelectionRuleServiceImpl.validate 五类规则：专业(1)/年级(2)/院系(3)/人数上限(4)/先修课(5)，
 * 以及空规则短路、空 restrictValue 跳过、非法配置容错、多规则违规聚合。
 * 学生信息来自 yu-sam（majorId/deptId/enrollmentYear）。</p>
 */
@ExtendWith(MockitoExtension.class)
class TpmSelectionRuleServiceImplTest {

    @Mock private TpmSelectionRuleMapper tpmSelectionRuleMapper;
    @Mock private SamStudentMapper samStudentMapper;
    @Mock private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @InjectMocks
    private TpmSelectionRuleServiceImpl service;

    private TpmSelectionRule rule(String type, String restrictValue) {
        TpmSelectionRule r = new TpmSelectionRule();
        r.setRuleId(1L);
        r.setRoundId(1L);
        r.setRuleName("规则" + type);
        r.setRuleType(type);
        r.setRestrictValue(restrictValue);
        r.setStatus("0");
        return r;
    }

    private SamStudent student(Long majorId, Long deptId, String year) {
        SamStudent s = new SamStudent();
        s.setStudentId(101L);
        s.setMajorId(majorId);
        s.setDeptId(deptId);
        s.setEnrollmentYear(year);
        return s;
    }

    @Test
    @DisplayName("轮次下无启用规则：直接通过且不查学生信息")
    void validate_noRules_returnsEmpty() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any())).thenReturn(new ArrayList<>());

        List<String> v = service.validate(1L, 101L, 20L);

        assertTrue(v.isEmpty());
        verify(samStudentMapper, never()).selectSamStudentByStudentId(anyLong());
    }

    @Test
    @DisplayName("restrictValue 为空的规则跳过，不产生违规")
    void validate_blankRestrictValue_skipped() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("1", "  "), rule("2", null)));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        assertTrue(service.validate(1L, 101L, 20L).isEmpty());
    }

    // ==================== 规则1：专业限制 ====================

    @Test
    @DisplayName("专业在允许列表内：通过（含逗号分隔带空格）")
    void validate_majorMatched() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("1", "4, 5 ,6")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        assertTrue(service.validate(1L, 101L, 20L).isEmpty());
    }

    @Test
    @DisplayName("专业不在允许列表：违规")
    void validate_majorNotMatched() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("1", "4,6")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        List<String> v = service.validate(1L, 101L, 20L);

        assertEquals(1, v.size());
        assertTrue(v.get(0).contains("指定专业"));
    }

    @Test
    @DisplayName("学生信息不存在：受限规则一律违规（安全兜底）")
    void validate_studentNull_violatesRestrictedRules() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("1", "4,6"), rule("2", "2023"), rule("3", "3")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(null);

        assertEquals(3, service.validate(1L, 101L, 20L).size());
    }

    // ==================== 规则2：年级限制 ====================

    @Test
    @DisplayName("年级匹配：通过；不匹配：违规")
    void validate_grade() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("2", "2023")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));
        assertTrue(service.validate(1L, 101L, 20L).isEmpty());

        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2022"));
        List<String> v = service.validate(1L, 101L, 20L);
        assertEquals(1, v.size());
        assertTrue(v.get(0).contains("2023级"));
    }

    // ==================== 规则3：院系限制 ====================

    @Test
    @DisplayName("院系不在允许列表：违规")
    void validate_deptNotMatched() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("3", "7,8")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        List<String> v = service.validate(1L, 101L, 20L);

        assertEquals(1, v.size());
        assertTrue(v.get(0).contains("指定院系"));
    }

    // ==================== 规则4：人数上限 ====================

    @Test
    @DisplayName("容量未满：通过；已满（enrolled>=limit）：违规")
    void validate_capacity() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("4", "50")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        when(tpmSelectionEnrollmentMapper.selectCountByOffering(20L)).thenReturn(49);
        assertTrue(service.validate(1L, 101L, 20L).isEmpty());

        when(tpmSelectionEnrollmentMapper.selectCountByOffering(20L)).thenReturn(50);
        List<String> v = service.validate(1L, 101L, 20L);
        assertEquals(1, v.size());
        assertTrue(v.get(0).contains("容量已满"));
    }

    @Test
    @DisplayName("人数上限配置非法：跳过该规则不抛异常")
    void validate_capacityInvalidNumber_skipped() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("4", "abc")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        assertTrue(service.validate(1L, 101L, 20L).isEmpty());
        verify(tpmSelectionEnrollmentMapper, never()).selectCountByOffering(anyLong());
    }

    // ==================== 规则5：先修课程 ====================

    @Test
    @DisplayName("先修课全部完成：通过；有未完成：违规")
    void validate_prerequisite() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any()))
                .thenReturn(Arrays.asList(rule("5", "100,200")));
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));

        when(tpmSelectionEnrollmentMapper.countCompletedCourses(anyLong(), anyList())).thenReturn(2);
        assertTrue(service.validate(1L, 101L, 20L).isEmpty());

        when(tpmSelectionEnrollmentMapper.countCompletedCourses(anyLong(), anyList())).thenReturn(1);
        List<String> v = service.validate(1L, 101L, 20L);
        assertEquals(1, v.size());
        assertTrue(v.get(0).contains("未完成先修课程"));
    }

    // ==================== 多规则聚合 ====================

    @Test
    @DisplayName("多规则同时违规：全部聚合上报")
    void validate_multipleViolations_aggregated() {
        when(tpmSelectionRuleMapper.selectTpmSelectionRuleList(any())).thenReturn(Arrays.asList(
                rule("1", "4,6"),      // 专业不符
                rule("2", "2021"),     // 年级不符
                rule("4", "10")));     // 容量已满
        when(samStudentMapper.selectSamStudentByStudentId(101L)).thenReturn(student(5L, 3L, "2023"));
        when(tpmSelectionEnrollmentMapper.selectCountByOffering(20L)).thenReturn(10);

        assertEquals(3, service.validate(1L, 101L, 20L).size());
    }
}
