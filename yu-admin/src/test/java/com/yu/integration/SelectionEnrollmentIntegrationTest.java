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
 * G4 首批集成测试 · 选课容量扣减（V5.0 §7.2 G4 / 债务 D11）。
 *
 * <p>走真实装配：TPM 服务 + MyBatis/Druid（yutest 口径的业务库）+ Redis DECR/INCR 原子扣减。
 * fixture 基线（testdata/seed-selection.sql）：轮次 9711 开放、开课 9721（容量 60）、学生 101。
 * 断言口径：选课成功后 DB 报名行 +1 且 Redis 剩余容量 = 容量上限 - 实时报名人数；
 * 容量耗尽场景拒绝并回补（扣减-恢复成对，Redis 键回到 0）。
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
        // 清运行期增量：本学生本轮回写的报名行 + 相关容量/学生缓存（幂等，可重复跑）
        jdbc().update("delete from tpm_selection_enrollment where round_id = ? and student_id = ?", ROUND_ID, STUDENT_ID);
        cacheManager.clearCapacityCache(OFFERING_ID);
        cacheManager.clearCapacityCache(FULL_OFFERING_ID);
        cacheManager.clearStudentCache(STUDENT_ID, ROUND_ID);
    }

    @Test
    @DisplayName("选课成功：DB 落报名行，Redis 容量精确扣减 1（= 上限 - 实时已报人数）")
    void enrollSuccessDecrementsRedisCapacity()
    {
        AjaxResult result = enrollmentService.enrollWithValidation(STUDENT_ID, OFFERING_ID, ROUND_ID);
        assertEquals(200, ((Number) result.get("code")).intValue(), "选课应成功: " + result.get("msg"));

        Integer rows = jdbc().queryForObject(
                "select count(*) from tpm_selection_enrollment where round_id = ? and student_id = ? and course_offering_id = ?",
                Integer.class, ROUND_ID, STUDENT_ID, OFFERING_ID);
        assertEquals(1, rows.intValue(), "报名行应恰好落库 1 条");

        Integer enrolled = jdbc().queryForObject(
                "select count(*) from tpm_selection_enrollment where course_offering_id = ?", Integer.class, OFFERING_ID);
        Integer max = jdbc().queryForObject(
                "select max_students from tpm_course_offering where offering_id = ?", Integer.class, OFFERING_ID);
        Long remaining = cacheManager.getCapacity(OFFERING_ID);
        assertEquals(max - enrolled, remaining.longValue(), "Redis 剩余容量应等于 上限-已报名人数");
    }

    @Test
    @DisplayName("容量耗尽：拒绝选课且扣减被回补（Redis 键回到 0，不落报名行）")
    void enrollRejectedWhenCapacityExhaustedAndRestored()
    {
        cacheManager.setCapacity(FULL_OFFERING_ID, 0);

        AjaxResult result = enrollmentService.enrollWithValidation(STUDENT_ID, FULL_OFFERING_ID, ROUND_ID);
        assertEquals(500, ((Number) result.get("code")).intValue(), "满员应拒绝");
        assertTrue(String.valueOf(result.get("msg")).contains("已满"), "拒绝原因应为课程已满");

        assertEquals(0L, cacheManager.getCapacity(FULL_OFFERING_ID).longValue(), "扣减-恢复应成对，容量键回到 0");
        Integer rows = jdbc().queryForObject(
                "select count(*) from tpm_selection_enrollment where round_id = ? and course_offering_id = ?",
                Integer.class, ROUND_ID, FULL_OFFERING_ID);
        assertEquals(0, rows.intValue(), "拒绝路径不应留下报名行");
    }
}
