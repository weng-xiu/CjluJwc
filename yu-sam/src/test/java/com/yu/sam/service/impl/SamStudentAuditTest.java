package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.yu.common.utils.bean.FieldChange;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.system.service.IDataChangeAuditService;

/**
 * 学籍修改字段级留痕服务层测试（V4.1 §6.2 K1 合规③接线扩展）。
 *
 * <p>校验 updateSamStudent 在修改成功后，以「库中旧值 vs 提交新值」比对产出差异并写流水；
 * 身份证等敏感字段旧/新值须脱敏后落库；无差异或更新 0 行时不留痕。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentAuditTest {

    @Mock
    private SamStudentMapper samStudentMapper;
    @Mock
    private SamStatusChangeMapper samStatusChangeMapper;
    @Mock
    private SamCertificateMapper samCertificateMapper;
    @Mock
    private IDataChangeAuditService dataChangeAuditService;

    @InjectMocks
    private SamStudentServiceImpl service;

    private SamStudent persisted() {
        SamStudent old = new SamStudent();
        old.setStudentId(1L);
        old.setStudentNo("2026001");
        old.setStudentName("张三");
        old.setIdCard("420101199001011234");
        old.setStudentStatus("0");
        old.setStatus("0");
        return old;
    }

    @Test
    @DisplayName("身份证变更 → 留痕且旧/新值脱敏，明文不落库")
    void updateIdCardAuditedMasked() {
        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(persisted());
        when(samStudentMapper.updateSamStudent(any(SamStudent.class))).thenReturn(1);

        SamStudent upd = new SamStudent();
        upd.setStudentId(1L);
        upd.setIdCard("420101199001015678"); // 仅提交身份证变更

        int rows = service.updateSamStudent(upd);
        assertEquals(1, rows);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sam_student"), eq("学生学籍"), eq("1"), captor.capture());

        List<FieldChange> changes = captor.getValue();
        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals("idCard", c.getFieldName());
        assertEquals("身份证号", c.getFieldLabel(), "标签应来自 @Excel(name)");
        assertFalse(c.getOldValue().contains("2010119900101"), "旧值须脱敏，不得含明文中段");
        assertFalse(c.getNewValue().contains("2010119900101"), "新值须脱敏，不得含明文中段");
        assertTrue(c.getOldValue().startsWith("4") && c.getOldValue().endsWith("4"), "掩码保留首尾");
    }

    @Test
    @DisplayName("非敏感字段学籍状态变更 → 明文留痕旧0新1")
    void updateStatusAuditedPlain() {
        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(persisted());
        when(samStudentMapper.updateSamStudent(any(SamStudent.class))).thenReturn(1);

        SamStudent upd = new SamStudent();
        upd.setStudentId(1L);
        upd.setStudentStatus("1"); // 在读→休学

        service.updateSamStudent(upd);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sam_student"), eq("学生学籍"), eq("1"), captor.capture());

        List<FieldChange> changes = captor.getValue();
        assertEquals(1, changes.size());
        assertEquals("studentStatus", changes.get(0).getFieldName());
        assertEquals("0", changes.get(0).getOldValue());
        assertEquals("1", changes.get(0).getNewValue());
    }

    @Test
    @DisplayName("提交值与库中旧值一致 → 无差异，留空列表")
    void updateNoChangeProducesEmptyDiff() {
        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(persisted());
        when(samStudentMapper.updateSamStudent(any(SamStudent.class))).thenReturn(1);

        SamStudent upd = new SamStudent();
        upd.setStudentId(1L);
        upd.setStudentStatus("0"); // 与旧值相同
        upd.setStudentName("张三");

        service.updateSamStudent(upd);

        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sam_student"), eq("学生学籍"), eq("1"), captor.capture());
        assertTrue(captor.getValue().isEmpty(), "无差异应产出空列表");
    }

    @Test
    @DisplayName("更新影响 0 行（未真正修改）→ 不写流水")
    void updateZeroRowsSkipsAudit() {
        when(samStudentMapper.selectSamStudentByStudentId(anyLong())).thenReturn(persisted());
        when(samStudentMapper.updateSamStudent(any(SamStudent.class))).thenReturn(0);

        SamStudent upd = new SamStudent();
        upd.setStudentId(1L);
        upd.setStudentStatus("1");

        service.updateSamStudent(upd);

        verify(dataChangeAuditService, never()).recordDiff(any(), any(), any(), any());
    }
}
