package com.yu.aem.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.mapper.AemGpaAlgorithmConfigMapper;
import com.yu.aem.service.IAemGradeWeightService;
import com.yu.aem.strategy.GpaStrategyFactory;
import com.yu.common.utils.bean.FieldChange;
import com.yu.system.service.IDataChangeAuditService;

/**
 * 成绩修改字段级留痕服务层测试（V4.0 §6.2 K1 合规③）。
 *
 * <p>校验 updateAemGradeRecord 在修改成功后，以「库中旧值 vs 提交新值」比对产出差异，
 * 并调用 {@link IDataChangeAuditService#recordDiff} 写入流水；无实际差异时不留痕。
 * 覆盖验收口径「任一成绩变更可查旧值新值与操作人」的服务侧契约。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeRecordAuditTest {

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

    @InjectMocks
    private AemGradeRecordServiceImpl service;

    private AemGradeRecord persisted() {
        AemGradeRecord old = new AemGradeRecord();
        old.setGradeId(1L);
        old.setStudentId(100L);
        old.setCourseId(200L);
        old.setSemesterId(3L);
        old.setRegularScore(70.0);
        old.setExamScore(80.0);
        old.setTotalScore(85.0);
        old.setIsReviewed("0");
        old.setSubmitStatus("0");
        old.setStatus("0");
        return old;
    }

    @Test
    @DisplayName("成绩总分变更 → 留痕旧值85新值90，实体类型与业务主键正确")
    void updateGradeRecordsFieldDiff() {
        when(aemGradeRecordMapper.selectAemGradeRecordByGradeId(1L)).thenReturn(persisted());
        when(aemGradeRecordMapper.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        AemGradeRecord upd = new AemGradeRecord();
        upd.setGradeId(1L);
        upd.setTotalScore(90.0); // 仅提交总分变更，其余字段 null（选择性更新，不留痕）

        int rows = service.updateAemGradeRecord(upd);
        assertEquals(1, rows);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("aem_grade_record"), eq("成绩记录"), eq("1"), captor.capture());

        List<FieldChange> changes = captor.getValue();
        assertEquals(1, changes.size(), "仅总分一个字段有差异");
        FieldChange c = changes.get(0);
        assertEquals("totalScore", c.getFieldName());
        assertEquals("总成绩", c.getFieldLabel(), "标签应来自 @Excel(name)");
        assertEquals("85.0", c.getOldValue());
        assertEquals("90.0", c.getNewValue());
    }

    @Test
    @DisplayName("提交值与库中旧值完全一致 → 无差异，不留痕")
    void updateGradeNoChangeDoesNotAudit() {
        when(aemGradeRecordMapper.selectAemGradeRecordByGradeId(1L)).thenReturn(persisted());
        when(aemGradeRecordMapper.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(1);

        AemGradeRecord upd = new AemGradeRecord();
        upd.setGradeId(1L);
        upd.setTotalScore(85.0); // 与旧值相同（数值语义 85==85.0）
        upd.setRegularScore(70.0);

        service.updateAemGradeRecord(upd);

        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("aem_grade_record"), eq("成绩记录"), eq("1"), captor.capture());
        assertTrue(captor.getValue().isEmpty(), "无差异应产出空列表");
    }

    @Test
    @DisplayName("更新影响 0 行（未真正修改）→ 不写流水")
    void updateZeroRowsSkipsAudit() {
        when(aemGradeRecordMapper.selectAemGradeRecordByGradeId(anyLong())).thenReturn(persisted());
        when(aemGradeRecordMapper.updateAemGradeRecord(any(AemGradeRecord.class))).thenReturn(0);

        AemGradeRecord upd = new AemGradeRecord();
        upd.setGradeId(1L);
        upd.setTotalScore(90.0);

        service.updateAemGradeRecord(upd);
        verify(dataChangeAuditService, never()).recordDiff(any(), any(), any(), any());
    }
}
