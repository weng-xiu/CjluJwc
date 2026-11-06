package com.yu.aem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.aem.domain.AemGradeImportResult;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.mapper.AemGpaAlgorithmConfigMapper;
import com.yu.aem.service.IAemGradeWeightService;
import com.yu.aem.strategy.GpaStrategyFactory;
import com.yu.aem.strategy.IGpaCalculationStrategy;
import com.yu.common.exception.ServiceException;
import com.yu.system.service.IDataChangeAuditService;

/**
 * E 档：Excel 成绩重复导入的「跳过已定稿」服务层测试。
 *
 * <p>口径（已拍板）：同一 (学生, 课程, 学期, 考试类型) 已有成绩时，
 * 未提交(0)/已驳回(3) 覆盖为新值；已提交待审(1)/已锁定(2) 保持原值，
 * 不进 batchInsert，并把 Excel 行号回显给操作人。</p>
 *
 * <p>SQL 侧的状态谓词由 {@link com.yu.aem.mapper.AemGradeRecordMapperXmlTest} 锁住，
 * 本测试锁住「服务层是否真的把这些行剔除了」——查询与写入之间存在并发提交/锁定的窗口，
 * 两道防线都要在。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeRecordImportTest
{
    @Mock
    private AemGradeRecordMapper aemGradeRecordMapper;
    @Mock
    private AemGradeReviewMapper aemGradeReviewMapper;
    @Mock
    private AemGpaAlgorithmConfigMapper aemGpaAlgorithmConfigMapper;
    @Mock
    private GpaStrategyFactory gpaStrategyFactory;
    @Mock
    private IAemGradeWeightService aemGradeWeightService;
    @Mock
    private IDataChangeAuditService dataChangeAuditService;
    @Mock
    private IGpaCalculationStrategy gpaStrategy;

    @InjectMocks
    private AemGradeRecordServiceImpl service;

    private static final String ALGORITHM = "CN_STANDARD";

    @BeforeEach
    void stubDependencies()
    {
        when(gpaStrategyFactory.getStrategy(anyString())).thenReturn(gpaStrategy);
        when(gpaStrategy.calculate(anyDouble())).thenReturn(3.0);
        when(gpaStrategy.getGradeLevel(anyDouble())).thenReturn("C");
        // 默认权重 30/70：80*0.3 + 90*0.7 = 87.0
        when(aemGradeWeightService.resolveRatios(anyLong())).thenReturn(new double[] { 0.3, 0.7 });
    }

    private AemGradeRecord row(long studentId, long courseId, long semesterId)
    {
        AemGradeRecord record = new AemGradeRecord();
        record.setStudentId(studentId);
        record.setCourseId(courseId);
        record.setSemesterId(semesterId);
        record.setRegularScore(80.0);
        record.setExamScore(90.0);
        return record;
    }

    private AemGradeRecord finalized(AemGradeRecord origin, String submitStatus)
    {
        AemGradeRecord key = new AemGradeRecord();
        key.setStudentId(origin.getStudentId());
        key.setCourseId(origin.getCourseId());
        key.setSemesterId(origin.getSemesterId());
        key.setExamType(origin.getExamType());
        key.setSubmitStatus(submitStatus);
        return key;
    }

    @Test
    @DisplayName("混合导入：已锁定行被剔除，其余照常写入，并回显 Excel 第 3 行")
    void skipFinalizedRowKeepsOthers()
    {
        AemGradeRecord first = row(101L, 201L, 1L);
        AemGradeRecord locked = row(102L, 201L, 1L);
        AemGradeRecord third = row(103L, 201L, 1L);
        List<AemGradeRecord> list = new ArrayList<>(List.of(first, locked, third));

        when(aemGradeRecordMapper.selectLockedGradeKeys(anyList()))
                .thenReturn(List.of(finalized(locked, "2")));
        when(aemGradeRecordMapper.batchInsert(anyList())).thenReturn(2);

        AemGradeImportResult result = service.importGrade(list, "tester", ALGORITHM);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AemGradeRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(aemGradeRecordMapper).batchInsert(captor.capture());
        List<AemGradeRecord> written = captor.getValue();
        assertEquals(2, written.size(), "已锁定行不得进入 batchInsert");
        assertTrue(written.stream().noneMatch(r -> Long.valueOf(102L).equals(r.getStudentId())),
                "被剔除的应恰好是锁定行：" + written);

        assertEquals(2, result.getProcessed());
        assertEquals(1, result.getSkipped());
        assertEquals(List.of(3), result.getSkippedRows(), "列表下标 1 对应 Excel 第 3 行（表头占第 1 行）");
        assertEquals(2, result.getAffectedRows());
        // 校验与算分仍作用于全部导入行（锁定行也要回显行号，不能因跳过而漏算）
        assertEquals("0", locked.getExamType(), "examType 空值须归一为 '0'");
    }

    @Test
    @DisplayName("整批都已定稿：不调用 batchInsert，处理 0 条并回显全部行号")
    void allFinalizedSkipsInsert()
    {
        AemGradeRecord one = row(101L, 201L, 1L);
        AemGradeRecord two = row(102L, 201L, 1L);
        List<AemGradeRecord> list = new ArrayList<>(List.of(one, two));

        when(aemGradeRecordMapper.selectLockedGradeKeys(anyList()))
                .thenReturn(List.of(finalized(one, "1"), finalized(two, "2")));

        AemGradeImportResult result = service.importGrade(list, "tester", ALGORITHM);

        verify(aemGradeRecordMapper, never()).batchInsert(anyList());
        assertEquals(0, result.getProcessed());
        assertEquals(2, result.getSkipped());
        assertEquals(List.of(2, 3), result.getSkippedRows());
        assertEquals(0, result.getAffectedRows());
    }

    @Test
    @DisplayName("无定稿行：全部写入，无跳过明细")
    void noneFinalizedWritesAll()
    {
        AemGradeRecord one = row(101L, 201L, 1L);
        AemGradeRecord two = row(102L, 202L, 2L);
        List<AemGradeRecord> list = new ArrayList<>(List.of(one, two));

        when(aemGradeRecordMapper.selectLockedGradeKeys(anyList())).thenReturn(List.of());
        when(aemGradeRecordMapper.batchInsert(anyList())).thenReturn(2);

        AemGradeImportResult result = service.importGrade(list, "tester", ALGORITHM);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AemGradeRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(aemGradeRecordMapper).batchInsert(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(0, result.getSkipped());
        assertTrue(result.getSkippedRows().isEmpty());
        assertEquals("", result.skippedRowSummary(10));

        // 一次查询覆盖本批涉及的所有学期，不逐行查库
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> semesters = ArgumentCaptor.forClass(List.class);
        verify(aemGradeRecordMapper).selectLockedGradeKeys(semesters.capture());
        assertEquals(List.of(1L, 2L), semesters.getValue());
    }

    @Test
    @DisplayName("历史行 exam_type 为 NULL：仍按正考 '0' 判定为已定稿并跳过")
    void nullExamTypeOnPersistedRowStillMatches()
    {
        AemGradeRecord imported = row(101L, 201L, 1L);
        List<AemGradeRecord> list = new ArrayList<>(List.of(imported));

        AemGradeRecord persisted = finalized(imported, "2");
        persisted.setExamType(null); // 老数据：库里存的是 NULL，导入给的是 '0'
        when(aemGradeRecordMapper.selectLockedGradeKeys(anyList())).thenReturn(List.of(persisted));

        AemGradeImportResult result = service.importGrade(list, "tester", ALGORITHM);

        verify(aemGradeRecordMapper, never()).batchInsert(anyList());
        assertEquals(1, result.getSkipped());
    }

    @Test
    @DisplayName("空导入：直接拒绝，不查库")
    void emptyImportRejected()
    {
        assertThrows(ServiceException.class, () -> service.importGrade(List.of(), "tester", ALGORITHM));
        verify(aemGradeRecordMapper, never()).selectLockedGradeKeys(any());
    }

    @Test
    @DisplayName("跳过行号简报：超过上限时只列前 N 个并给出总数")
    void skippedRowSummaryTruncates()
    {
        AemGradeImportResult result = new AemGradeImportResult();
        for (int i = 2; i <= 13; i++)
        {
            result.addSkippedRow(i);
        }
        assertEquals("2, 3, 4 等 12 行", result.skippedRowSummary(3));
        assertEquals(12, result.getSkipped());
    }
}
