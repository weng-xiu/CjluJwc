package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.yu.common.core.domain.AjaxResult;
import com.yu.framework.cache.SelectionCacheManager;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * G4 首批集成测试 · 选课容量扣减（V5.0 §7.2 G4 / 债务 D11；T-3 容量改 DB 条件更新后重写）。
 *
 * <p>走真实装配：TPM 服务 + MyBatis/Druid（yutest 口径的业务库）。
 * fixture 基线（testdata/seed-selection.sql）：轮次 9711 开放、开课 9721（容量 60）、学生 101。
 * 断言口径（T-3）：容量真相是 tpm_course_offering.enrolled_count，选课成功后它必须等于
 * 报名表里 result_status='1' 且未删除的行数；满员场景直接把 enrolled_count 抬到 max_students
 * 造前置（不再依赖 Redis 键），被拒后 enrolled_count 不变且不落报名行。
 */
class SelectionEnrollmentIntegrationTest extends AbstractIntegrationTest
{
    private static final long ROUND_ID = 9711L;
    private static final long STUDENT_ID = 101L;
    private static final long OFFERING_ID = 9721L;
    private static final long FULL_OFFERING_ID = 9723L;

    @Autowired
    private ITpmSelectionEnrollmentService enrollmentService;

    @Autowired
    private SelectionCacheManager cacheManager;

    @BeforeEach
    @AfterEach
    void resetRunState()
    {
        // 清运行期增量：本学生本轮回写的报名行 + 学生缓存（幂等，可重复跑）
        jdbc().update("delete from tpm_selection_enrollment where round_id = ? and student_id = ?", ROUND_ID, STUDENT_ID);
        // T-3：enrolled_count 是跨用例共享的列，复位时按报名表重算（与生产端 recountEnrolled 同口径）
        jdbc().update("update tpm_course_offering set enrolled_count = "
                + "(select count(1) from tpm_selection_enrollment e where e.course_offering_id = tpm_course_offering.offering_id "
                + "and e.result_status = '1' and e.del_flag = '0') "
                + "where offering_id in (?, ?)", OFFERING_ID, FULL_OFFERING_ID);
        cacheManager.clearStudentCache(STUDENT_ID, ROUND_ID);
    }

    @Test
    @DisplayName("选课成功：DB 落报名行，enrolled_count 原子 +1且等于在册人数")
    void enrollSuccessIncrementsEnrolledCount()
    {
        Integer before = jdbc().queryForObject(
                "select enrolled_count from tpm_course_offering where offering_id = ?", Integer.class, OFFERING_ID);

        AjaxResult result = enrollmentService.enrollWithValidation(STUDENT_ID, OFFERING_ID, ROUND_ID);
        assertEquals(200, ((Number) result.get("code")).intValue(), "选课应成功: " + result.get("msg"));

        Integer rows = jdbc().queryForObject(
                "select count(*) from tpm_selection_enrollment where round_id = ? and student_id = ? and course_offering_id = ?",
                Integer.class, ROUND_ID, STUDENT_ID, OFFERING_ID);
        assertEquals(1, rows.intValue(), "报名行应恰好落库 1 条");

        Integer enrolledCount = jdbc().queryForObject(
                "select enrolled_count from tpm_course_offering where offering_id = ?", Integer.class, OFFERING_ID);
        Integer admitted = jdbc().queryForObject(
                "select count(1) from tpm_selection_enrollment where course_offering_id = ? "
                        + "and result_status = '1' and del_flag = '0'", Integer.class, OFFERING_ID);
        assertEquals(before.intValue() + 1, enrolledCount.intValue(), "enrolled_count 应精确 +1，不多不少");
        assertEquals(admitted.intValue(), enrolledCount.intValue(), "enrolled_count 必须等于报名表在册人数");
    }

    @Test
    @DisplayName("容量耗尽：DB 条件更新拒绝，enrolled_count 保持上限且不落报名行")
    void enrollRejectedWhenCapacityExhausted()
    {
        // 造前置：把 enrolled_count 抬到上限（不依赖 Redis 键是否存在）
        jdbc().update("update tpm_course_offering set enrolled_count = max_students where offering_id = ?", FULL_OFFERING_ID);
        Integer max = jdbc().queryForObject(
                "select max_students from tpm_course_offering where offering_id = ?", Integer.class, FULL_OFFERING_ID);

        AjaxResult result = enrollmentService.enrollWithValidation(STUDENT_ID, FULL_OFFERING_ID, ROUND_ID);
        assertEquals(500, ((Number) result.get("code")).intValue(), "满员应拒绝");
        assertTrue(String.valueOf(result.get("msg")).contains("已满"), "拒绝原因应为课程已满");

        Integer enrolledCount = jdbc().queryForObject(
                "select enrolled_count from tpm_course_offering where offering_id = ?", Integer.class, FULL_OFFERING_ID);
        assertEquals(max.intValue(), enrolledCount.intValue(), "被拒后 enrolled_count 不得被改动");
        Integer rows = jdbc().queryForObject(
                "select count(*) from tpm_selection_enrollment where round_id = ? and course_offering_id = ?",
                Integer.class, ROUND_ID, FULL_OFFERING_ID);
        assertEquals(0, rows.intValue(), "拒绝路径不应留下报名行");
    }
}
