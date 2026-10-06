package com.yu.tpm.schedule.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * N4 约束 DSL 解析器单元验证：默认值、覆盖、校验错误聚合与边界。
 *
 * @author N4
 */
class ConstraintDslParserTest
{
    @Test
    @DisplayName("空/空白 DSL 解析为默认策略")
    void blankGivesDefaults()
    {
        ScheduleConstraintPolicy p = ConstraintDslParser.parse("   ");
        assertTrue(p.isHardForbidden());
        assertFalse(p.isHardSameCampus());
        assertEquals(100, p.getSoftLabMatch());
        assertEquals(10, p.getCapacityFitDivisor());

        assertEquals(ScheduleConstraintPolicy.class.getName(),
                ConstraintDslParser.parse(null).getClass().getName());
    }

    @Test
    @DisplayName("键值覆盖生效且大小写不敏感")
    void overridesApply()
    {
        ScheduleConstraintPolicy p = ConstraintDslParser.parse(
                "SOFT.LABMATCH=120; hard.samecampus=TRUE; pref.dailyBalance=0; hard.maxSessionsPerDay=2");
        assertEquals(120, p.getSoftLabMatch());
        assertTrue(p.isHardSameCampus());
        assertFalse(p.isDailyBalance());
        assertEquals(2, p.getHardMaxSessionsPerDay());
    }

    @Test
    @DisplayName("非法输入聚合多条校验错误")
    void collectsAllErrors()
    {
        ConstraintDslException ex = assertThrows(ConstraintDslException.class,
                () -> ConstraintDslParser.parse("unknown.key=1; soft.labmatch=abc; soft.capacityFitDivisor=0; broken"));
        List<String> errors = ex.getErrors();
        assertTrue(errors.stream().anyMatch(e -> e.contains("未知约束键")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("需要整数")));
        assertTrue(errors.stream().anyMatch(e -> e.contains(">= 1")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("缺少 '='")));
        assertEquals(4, errors.size());
    }

    @Test
    @DisplayName("validate 不抛异常，仅返回错误列表")
    void validateReturnsErrors()
    {
        assertTrue(ConstraintDslParser.validate("hard.forbidden=false").isEmpty());
        assertEquals(1, ConstraintDslParser.validate("hard.forbidden=maybe").size());
        assertEquals(1, ConstraintDslParser.validate("hard.maxSessionsPerDay=-1").size());
    }
}
