package com.yu.aem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.aem.domain.AemGradeRecord;
import com.yu.common.utils.bean.FieldChange;
import com.yu.common.utils.bean.FieldDiffUtils;

/**
 * 字段级差异比对工具测试（V4.0 K1 合规③比对内核，落于 yu-aem 复用其测试依赖）。
 */
class FieldDiffUtilsTest {

    private AemGradeRecord base() {
        AemGradeRecord r = new AemGradeRecord();
        r.setGradeId(1L);
        r.setTotalScore(85.0);
        r.setRegularScore(70.0);
        r.setExamType("0");
        return r;
    }

    @Test
    @DisplayName("新值为 null 的字段跳过（选择性更新语义，不误记为改成空）")
    void nullNewValueSkipped() {
        AemGradeRecord old = base();
        AemGradeRecord neu = new AemGradeRecord();
        neu.setGradeId(1L); // 仅主键，其余提交值全 null
        List<FieldChange> changes = FieldDiffUtils.diff(old, neu);
        assertTrue(changes.isEmpty(), "未提交任何业务字段应无差异");
    }

    @Test
    @DisplayName("数值 85 与 85.0 视为相等，不产出变更")
    void numericEqualityByValue() {
        AemGradeRecord old = base();
        AemGradeRecord neu = base();
        neu.setTotalScore(85.0);
        assertTrue(FieldDiffUtils.diff(old, neu).isEmpty());
    }

    @Test
    @DisplayName("标签取自 @Excel(name)，如 totalScore→总成绩")
    void labelFromExcelAnnotation() {
        AemGradeRecord old = base();
        AemGradeRecord neu = base();
        neu.setTotalScore(90.0);
        List<FieldChange> changes = FieldDiffUtils.diff(old, neu);
        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals("totalScore", c.getFieldName());
        assertEquals("总成绩", c.getFieldLabel());
        assertEquals("85.0", c.getOldValue());
        assertEquals("90.0", c.getNewValue());
    }

    @Test
    @DisplayName("敏感字段旧/新值均掩码后落库")
    void sensitiveFieldMasked() {
        AemGradeRecord old = base();
        old.setExamType("0123");
        AemGradeRecord neu = base();
        neu.setExamType("4567");
        Set<String> sensitive = new HashSet<>();
        sensitive.add("examType");
        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, null, sensitive);
        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals("0**3", c.getOldValue(), "保留首尾、中间遮蔽");
        assertEquals("4**7", c.getNewValue());
    }

    @Test
    @DisplayName("BaseEntity 审计列（createTime）不参与比对")
    void auditColumnsExcluded() {
        AemGradeRecord old = base();
        old.setCreateTime(new java.util.Date(1000L));
        AemGradeRecord neu = base();
        neu.setTotalScore(90.0);
        neu.setCreateTime(new java.util.Date(999999L));
        List<String> names = FieldDiffUtils.diff(old, neu).stream()
                .map(FieldChange::getFieldName).collect(Collectors.toList());
        assertTrue(names.contains("totalScore"));
        assertTrue(!names.contains("createTime"), "createTime 属噪声字段应被排除");
    }

    @Test
    @DisplayName("trackedFields 限定比对范围")
    void trackedFieldsLimitsScope() {
        AemGradeRecord old = base();
        AemGradeRecord neu = base();
        neu.setTotalScore(90.0);
        neu.setRegularScore(60.0);
        Set<String> tracked = new HashSet<>();
        tracked.add("totalScore");
        List<String> names = FieldDiffUtils.diff(old, neu, tracked, null).stream()
                .map(FieldChange::getFieldName).collect(Collectors.toList());
        assertEquals(1, names.size());
        assertTrue(names.contains("totalScore"));
    }
}
