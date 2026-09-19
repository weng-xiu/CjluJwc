package com.yu.sam.workflow;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamStudent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S5 学籍异动审批回写联动 单元测试
 * 验收标准：审批后学籍状态自动变更；联动可验证
 */
class StatusChangeApprovalHandlerTest
{
    private StatusChangeApprovalHandler handler;

    @BeforeEach
    void setUp()
    {
        handler = new StatusChangeApprovalHandler();
    }

    // ===== resolveTargetStatus =====

    @Test
    @DisplayName("休学(0) -> 学籍状态1(休学)")
    void resolveStatus_leave()
    {
        assertEquals("1", handler.resolveTargetStatus("0", null));
    }

    @Test
    @DisplayName("复学(1) -> 学籍状态0(在读)")
    void resolveStatus_resume()
    {
        assertEquals("0", handler.resolveTargetStatus("1", null));
    }

    @Test
    @DisplayName("退学(3) -> 学籍状态2(退学)")
    void resolveStatus_drop()
    {
        assertEquals("2", handler.resolveTargetStatus("3", null));
    }

    @Test
    @DisplayName("显式newStatus优先于映射")
    void resolveStatus_explicitWins()
    {
        assertEquals("5", handler.resolveTargetStatus("0", "5"));
    }

    @Test
    @DisplayName("未知异动类型且无newStatus返回null")
    void resolveStatus_unknown()
    {
        assertNull(handler.resolveTargetStatus("9", null));
    }

    // ===== buildLinkageActions =====

    @Test
    @DisplayName("学籍状态为休学(1) -> 冻结选课+标记成绩+停缴")
    void linkage_freeze()
    {
        SamStudent student = new SamStudent();
        student.setStudentId(100L);
        List<String> actions = handler.buildLinkageActions(student, "1");
        assertEquals(3, actions.size());
        assertTrue(actions.contains("FREEZE_ENROLLMENT:100"));
        assertTrue(actions.contains("FLAG_GRADE:100"));
        assertTrue(actions.contains("STOP_PAYMENT:100"));
    }

    @Test
    @DisplayName("学籍状态为退学(2) -> 冻结选课+标记成绩+停缴")
    void linkage_drop()
    {
        SamStudent student = new SamStudent();
        student.setStudentId(200L);
        List<String> actions = handler.buildLinkageActions(student, "2");
        assertEquals(3, actions.size());
        assertTrue(actions.contains("FREEZE_ENROLLMENT:200"));
    }

    @Test
    @DisplayName("学籍状态为在读(0)复学 -> 解冻选课+恢复缴费")
    void linkage_unfreeze()
    {
        SamStudent student = new SamStudent();
        student.setStudentId(300L);
        List<String> actions = handler.buildLinkageActions(student, "0");
        assertEquals(2, actions.size());
        assertTrue(actions.contains("UNFREEZE_ENROLLMENT:300"));
        assertTrue(actions.contains("RESUME_PAYMENT:300"));
    }

    @Test
    @DisplayName("毕业(3) -> 无联动操作")
    void linkage_graduate()
    {
        SamStudent student = new SamStudent();
        student.setStudentId(400L);
        List<String> actions = handler.buildLinkageActions(student, "3");
        assertTrue(actions.isEmpty());
    }

    @Test
    @DisplayName("student为null -> 空列表")
    void linkage_nullStudent()
    {
        List<String> actions = handler.buildLinkageActions(null, "1");
        assertTrue(actions.isEmpty());
    }

    // ===== validate =====

    @Test
    @DisplayName("合法异动记录校验通过")
    void validate_ok()
    {
        SamStatusChange change = new SamStatusChange();
        change.setStudentId(1L);
        change.setChangeType("0");
        assertNull(handler.validate(change));
    }

    @Test
    @DisplayName("studentId为null校验失败")
    void validate_noStudent()
    {
        SamStatusChange change = new SamStatusChange();
        change.setChangeType("0");
        assertNotNull(handler.validate(change));
    }

    @Test
    @DisplayName("无效异动类型且无newStatus校验失败")
    void validate_badType()
    {
        SamStatusChange change = new SamStatusChange();
        change.setStudentId(1L);
        change.setChangeType("99");
        assertNotNull(handler.validate(change));
    }

    @Test
    @DisplayName("无效类型但有newStatus可以通过")
    void validate_hasNewStatus()
    {
        SamStatusChange change = new SamStatusChange();
        change.setStudentId(1L);
        change.setChangeType("99");
        change.setNewStatus("3");
        assertNull(handler.validate(change));
    }
}
