package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.service.IAemGradeReviewService;

/**
 * G4 首批集成测试 · 成绩审批回写（V5.0 §7.2 G4 / 债务 D11）。
 *
 * <p>走真实装配：AEM 复核服务 + MyBatis/Druid 落库回写。fixture 基线（testdata/seed-grade.sql）：
 * 草稿成绩 9731（总分 87，未审核）。审批通过后应回写 total_score=新分数、is_reviewed='1'；
 * 驳回则成绩原样不动。运行期增量（复核行 9781 + 成绩行改动）由用例自清理，复位到 seed 口径。
 *
 * <p>注：本批覆盖旧单级审批口径（approveReview，无 Flowable 依赖）；多级流程口径
 * （submitForApproval + 初审/终审）依赖流程定义部署，留待 G4 后续批次。
 */
class GradeReviewWritebackIntegrationTest extends AbstractIntegrationTest
{
    private static final long REVIEW_ID = 9781L;
    private static final long GRADE_ID = 9731L;
    private static final long STUDENT_ID = 101L;

    @Autowired
    private IAemGradeReviewService gradeReviewService;

    @BeforeEach
    @AfterEach
    void seedAndReset()
    {
        // 清复核运行期残留 + 成绩复位为 seed 基线（草稿：87 分、未审核）
        jdbc().update("delete from aem_grade_review where review_id = ?", REVIEW_ID);
        jdbc().update("update aem_grade_record set total_score = 87.00, is_reviewed = '0' where grade_id = ?", GRADE_ID);
        // 待审的成绩变更申请（review_type='0' 改分，未挂流程 → 走单级审批回写口径）
        jdbc().update("insert into aem_grade_review (review_id, grade_id, student_id, course_id, original_score, new_score,"
                + " review_reason, review_type, approve_status, create_by, create_time)"
                + " values (?, ?, ?, 9301, 87.00, 92.00, 'G4集成测试回写', '0', '0', 'weng', now())",
                REVIEW_ID, GRADE_ID, STUDENT_ID);
    }

    @Test
    @DisplayName("审批通过：复核单置'1'，成绩行回写新总分并置已审核")
    void approveWritesBackScoreAndReviewedFlag()
    {
        int rows = gradeReviewService.approveReview(REVIEW_ID, true, "admin", "同意回写");
        assertEquals(1, rows, "复核单审批应更新 1 行");

        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(REVIEW_ID);
        assertEquals("1", review.getApproveStatus(), "复核单应置为通过");
        assertEquals("admin", review.getApproveBy());

        Map<String, Object> grade = jdbc().queryForMap(
                "select total_score, is_reviewed from aem_grade_record where grade_id = ?", GRADE_ID);
        assertEquals(0, new BigDecimal("92.00").compareTo((BigDecimal) grade.get("total_score")),
                "成绩应回写为新分数 92.00");
        assertEquals("1", grade.get("is_reviewed"), "回写后应置已审核");
    }

    @Test
    @DisplayName("审批驳回：复核单置'2'，成绩保持原分未审核")
    void rejectKeepsGradeUntouched()
    {
        int rows = gradeReviewService.approveReview(REVIEW_ID, false, "admin", "证据不足");
        assertEquals(1, rows);

        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(REVIEW_ID);
        assertEquals("2", review.getApproveStatus(), "复核单应置为驳回");

        Map<String, Object> grade = jdbc().queryForMap(
                "select total_score, is_reviewed from aem_grade_record where grade_id = ?", GRADE_ID);
        assertEquals(0, new BigDecimal("87.00").compareTo((BigDecimal) grade.get("total_score")),
                "驳回不应改动成绩");
        assertEquals("0", grade.get("is_reviewed"), "驳回不应置已审核");
    }
}
