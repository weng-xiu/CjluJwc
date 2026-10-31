package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.yu.common.core.domain.AjaxResult;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * T-3 并发收口集成测试（真库 InnoDB 行锁口径）。
 *
 * <p>V5.26 把容量真相下沉到 {@code tpm_course_offering.enrolled_count} 后，单元测试只能证明
 * “调用了条件更新／调用了几次”，证明不了**并发下名额与报名记录是否仍然一一对应**。本类用
 * {@link CyclicBarrier} 把请求真正同时放行，对真实装配（Spring + MyBatis + MySQL）断言四件事：</p>
 *
 * <ol>
 *   <li>并发选课：成功数与 {@code enrolled_count}、在册行数三者都恰好等于容量，绝不多占；</li>
 *   <li>并发退课：同一记录被多路同时退时只允许一次成功，名额只能减一次（历史缺陷：先查后改 →
 *       多路都放行 → 多次 releaseSeat，{@code > 0} 守卫只防负数，防不住少计）；</li>
 *   <li>并发改选：同一记录多路同时改选时，目标课只 +1、原课只 -1；</li>
 *   <li>并发重复选课（P-2）：同一学生多路同时选同一门课，靠唯一键 uk_enroll_active
 *       只在册 1 行，且落败请求占的名额随事务一起回滚（不多占）；</li>
 *   <li>重复抽签（T-4）：首次抽签后轮次置已结束，第二次被状态机挡住，结果行与种子均不变。</li>
 * </ol>
 *
 * <p>fixture：轮次 9711（本类负责置回进行中）＋本类独占的开课 9731~9736，学生 ID 用 2xxx/3xxx/4xxx/5xxx/6xxx
 * 段（不存在于 sam_student，选课/退课链路不做学籍校验，但门数/冲突判定用 studentId 作为键，互不干扰）。
 * 每个用例前重建前置、用例后清场，保证可重复跑。</p>
 */
class SelectionConcurrencyIntegrationTest extends AbstractIntegrationTest
{
    private static final long ROUND_ID = 9711L;

    /** 并发选课：容量 4，放行 16 个不同学生 */
    private static final long ENROLL_OFFERING = 9731L;
    private static final int ENROLL_CAPACITY = 4;
    private static final int ENROLL_THREADS = 16;

    /** 并发退课：容量 8，先造 5 条在册记录，再让 8 路同时退同一条 */
    private static final long DROP_OFFERING = 9732L;
    private static final int DROP_SEED_ROWS = 5;
    private static final int DROP_THREADS = 8;

    /** 并发改选：源/目标各容量 4，一条在册记录被 8 路同时改选到目标课 */
    private static final long SWAP_SOURCE = 9733L;
    private static final long SWAP_TARGET = 9734L;
    private static final int SWAP_THREADS = 8;

    /** 重复抽签：容量 2 / 5 条报名 */
    private static final long LOTTERY_OFFERING = 9735L;
    private static final int LOTTERY_CAPACITY = 2;
    private static final long LOTTERY_SEED = 20261031L;

    /** 并发重复选课（P-2）：同一学生 8 路同时选同一门课，容量故意给到 20（足够全部占上名额） */
    private static final long DUPLICATE_OFFERING = 9736L;
    private static final int DUPLICATE_CAPACITY = 20;
    private static final int DUPLICATE_THREADS = 8;

    private static final long[] ALL_OFFERINGS = { ENROLL_OFFERING, DROP_OFFERING, SWAP_SOURCE, SWAP_TARGET, LOTTERY_OFFERING,
            DUPLICATE_OFFERING };

    @Autowired
    private ITpmSelectionEnrollmentService enrollmentService;

    @BeforeEach
    @AfterEach
    void resetFixture()
    {
        jdbc().update("delete from tpm_selection_enrollment where round_id = ?", ROUND_ID);
        for (long offeringId : ALL_OFFERINGS)
        {
            jdbc().update("delete from tpm_course_offering where offering_id = ?", offeringId);
        }
        // 轮次：本类用例反复在「进行中/已结束」之间切换，统一复位为进行中并开放退改选窗口
        jdbc().update("update tpm_selection_round set round_status = '1', lottery_seed = null, lottery_time = null, "
                + "allow_drop_adjust = '1', drop_adjust_start = null, drop_adjust_end = null where round_id = ?", ROUND_ID);

        // 本类独占的开课（course_id/teacher/campus 复用 97xx fixture 的既有取值）
        insertOffering(ENROLL_OFFERING, 9301L, ENROLL_CAPACITY);
        insertOffering(DROP_OFFERING, 9302L, 8);
        insertOffering(SWAP_SOURCE, 9301L, SWAP_THREADS);
        insertOffering(SWAP_TARGET, 9302L, SWAP_THREADS);
        insertOffering(LOTTERY_OFFERING, 9303L, LOTTERY_CAPACITY);
        insertOffering(DUPLICATE_OFFERING, 9301L, DUPLICATE_CAPACITY);
    }

    private void insertOffering(long offeringId, long courseId, int maxStudents)
    {
        jdbc().update("insert into tpm_course_offering (offering_id, semester_id, course_id, teacher_id, campus_id, "
                + "class_count, max_students, enrolled_count, offering_status, status, del_flag, create_by, create_time) "
                + "values (?, 1, ?, 100, 1, 1, ?, 0, '1', '0', '0', 'it', now())",
                offeringId, courseId, maxStudents);
    }

    /** 直接落一条在册报名行（绕过选课链路，用于构造退课/改选/抽签的前置） */
    private long seedEnrollment(long studentId, long offeringId)
    {
        jdbc().update("insert into tpm_selection_enrollment (round_id, student_id, course_offering_id, select_time, "
                + "lottery_result, result_status, status, del_flag, create_time) "
                + "values (?, ?, ?, now(), '0', '1', '0', '0', now())", ROUND_ID, studentId, offeringId);
        return jdbc().queryForObject("select enroll_id from tpm_selection_enrollment where round_id = ? and student_id = ?",
                Long.class, ROUND_ID, studentId);
    }

    private int enrolledCountOf(long offeringId)
    {
        return jdbc().queryForObject("select enrolled_count from tpm_course_offering where offering_id = ?",
                Integer.class, offeringId);
    }

    private int admittedRowsOf(long offeringId)
    {
        return jdbc().queryForObject("select count(1) from tpm_selection_enrollment "
                + "where course_offering_id = ? and result_status = '1' and del_flag = '0'", Integer.class, offeringId);
    }

    private int codeOf(AjaxResult result)
    {
        return ((Number) result.get(AjaxResult.CODE_TAG)).intValue();
    }

    /** 把 n 个任务同时放行（CyclicBarrier 保证真正并发，而不是线程池排队） */
    private <T> List<T> runConcurrently(int threads, List<Callable<T>> tasks) throws Exception
    {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try
        {
            CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : tasks)
            {
                futures.add(pool.submit(() ->
                {
                    barrier.await(30, TimeUnit.SECONDS);
                    return task.call();
                }));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures)
            {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("并发选课：16 路同时抢 4 个名额，成功数/在册行/enrolled_count 三者都恰好 4")
    void concurrentEnroll_neverOversells() throws Exception
    {
        List<Callable<AjaxResult>> tasks = new ArrayList<>();
        for (int i = 0; i < ENROLL_THREADS; i++)
        {
            long studentId = 2001L + i;
            tasks.add(() -> enrollmentService.enrollWithValidation(studentId, ENROLL_OFFERING, ROUND_ID));
        }

        List<AjaxResult> results = runConcurrently(ENROLL_THREADS, tasks);

        int success = 0;
        for (AjaxResult r : results)
        {
            if (codeOf(r) == 200)
            {
                success++;
            }
            else
            {
                String msg = String.valueOf(r.get(AjaxResult.MSG_TAG));
                assertTrue(msg.contains("已满"), "非满员原因造成的失败说明链路本身出了问题：" + msg);
            }
        }
        assertEquals(ENROLL_CAPACITY, success, "容量 4 只能有 4 个人选上");
        assertEquals(ENROLL_CAPACITY, enrolledCountOf(ENROLL_OFFERING), "enrolled_count 必须等于容量，不得超卖");
        assertEquals(ENROLL_CAPACITY, admittedRowsOf(ENROLL_OFFERING), "在册报名行数必须等于容量");
    }

    @Test
    @DisplayName("并发退课：同一记录 8 路同时退，只成功一次且名额只减一次")
    void concurrentDrop_releasesSeatOnce() throws Exception
    {
        long targetEnrollId = 0L;
        for (int i = 0; i < DROP_SEED_ROWS; i++)
        {
            long enrollId = seedEnrollment(3001L + i, DROP_OFFERING);
            if (i == 0)
            {
                targetEnrollId = enrollId;
            }
        }
        jdbc().update("update tpm_course_offering set enrolled_count = ? where offering_id = ?",
                DROP_SEED_ROWS, DROP_OFFERING);

        List<Callable<AjaxResult>> tasks = new ArrayList<>();
        for (int i = 0; i < DROP_THREADS; i++)
        {
            long enrollId = targetEnrollId;
            tasks.add(() -> enrollmentService.dropCourse(enrollId));
        }

        List<AjaxResult> results = runConcurrently(DROP_THREADS, tasks);

        int success = 0;
        for (AjaxResult r : results)
        {
            if (codeOf(r) == 200)
            {
                success++;
            }
        }
        assertEquals(1, success, "同一条报名记录只能被退掉一次，其余请求必须被状态机挡回");
        assertEquals(DROP_SEED_ROWS - 1, enrolledCountOf(DROP_OFFERING), "名额只能释放一次（重复释放会让计数少于真实在册数）");
        assertEquals(DROP_SEED_ROWS - 1, admittedRowsOf(DROP_OFFERING), "在册行应剩 4 条");
        assertEquals(enrolledCountOf(DROP_OFFERING), admittedRowsOf(DROP_OFFERING),
                "enrolled_count 必须始终等于在册报名行数");
    }

    @Test
    @DisplayName("并发改选：同一记录 8 路同时改选，目标课只 +1、原课只 -1")
    void concurrentSwap_occupiesTargetOnce() throws Exception
    {
        long enrollId = seedEnrollment(4001L, SWAP_SOURCE);
        jdbc().update("update tpm_course_offering set enrolled_count = 1 where offering_id = ?", SWAP_SOURCE);

        List<Callable<AjaxResult>> tasks = new ArrayList<>();
        for (int i = 0; i < SWAP_THREADS; i++)
        {
            long id = enrollId;
            tasks.add(() -> enrollmentService.swapCourse(id, SWAP_TARGET));
        }

        List<AjaxResult> results = runConcurrently(SWAP_THREADS, tasks);

        int success = 0;
        for (AjaxResult r : results)
        {
            if (codeOf(r) == 200)
            {
                success++;
            }
        }
        assertEquals(1, success, "同一条记录只能改选成功一次");
        assertEquals(1, enrolledCountOf(SWAP_TARGET), "目标课只应被占 1 个名额");
        assertEquals(1, admittedRowsOf(SWAP_TARGET), "目标课在册行只应有 1 条");
        assertEquals(0, enrolledCountOf(SWAP_SOURCE), "原课名额只应被释放一次");
        assertEquals(0, admittedRowsOf(SWAP_SOURCE), "原课不应再有在册行");
    }

    @Test
    @DisplayName("P-2 并发重复选课：同一学生 8 路同时选同一门课，只在册 1 行、名额只占 1 个")
    void concurrentDuplicateEnroll_keepsSingleRow() throws Exception
    {
        long studentId = 6001L;
        List<Callable<AjaxResult>> tasks = new ArrayList<>();
        for (int i = 0; i < DUPLICATE_THREADS; i++)
        {
            tasks.add(() ->
            {
                try
                {
                    return enrollmentService.enrollWithValidation(studentId, DUPLICATE_OFFERING, ROUND_ID);
                }
                catch (RuntimeException ex)
                {
                    // 唯一键冲突在最底层走抛出（ServiceException），以保证占名额那次 update 跟着回滚；
                    // 控制器层由 GlobalExceptionHandler 翻成 500 + 友好文案，这里等价模拟。
                    return AjaxResult.error(ex.getMessage());
                }
            });
        }

        List<AjaxResult> results = runConcurrently(DUPLICATE_THREADS, tasks);

        int success = 0;
        for (AjaxResult r : results)
        {
            if (codeOf(r) == 200)
            {
                success++;
            }
            else
            {
                // 落败请求有两条路径，都可接受：
                //   ① 先验重复（与已提交行比对）→「本轮次已选过该课程」；
                //   ② 先验没看到（与中签者真正同时通过检测）→ 唯一键冲突 →「请勿重复提交」。
                // InnoDB 行锁会把两路请求在 occupySeat 上串行化，所以实测几乎总走 ①；
                // ② 是为真正同瞬的窗口兜底的那一层。
                String msg = String.valueOf(r.get(AjaxResult.MSG_TAG));
                assertTrue(msg.contains("已选过该课程") || msg.contains("请勿重复提交"),
                        "非重复提交原因造成的失败说明链路本身出了问题：" + msg);
            }
        }
        assertEquals(1, success, "同一学生同一门课最多只能选上一次");
        assertEquals(1, admittedRowsOf(DUPLICATE_OFFERING), "在册报名行必须只有 1 条（DB 唯一键兜底）");
        assertEquals(1, enrolledCountOf(DUPLICATE_OFFERING),
                "名额只能占 1 个；大于 1 说明落败请求占的名额没跟着事务回滚");
    }

    @Test
    @DisplayName("重复抽签：首次抽签后第二次被状态机挡住，结果与种子均不变")
    void duplicateLottery_isIdempotent()
    {
        for (int i = 0; i < 5; i++)
        {
            seedEnrollment(5001L + i, LOTTERY_OFFERING);
        }
        jdbc().update("update tpm_course_offering set enrolled_count = 5 where offering_id = ?", LOTTERY_OFFERING);

        Map<String, Object> first = enrollmentService.runLottery(ROUND_ID, LOTTERY_SEED);
        assertEquals(LOTTERY_CAPACITY, first.get("successCount"), "5 报 2 容量：中签应为 2：" + first.get("message"));
        assertEquals(5 - LOTTERY_CAPACITY, first.get("failCount"), "落选应为 3：" + first.get("message"));
        assertEquals(LOTTERY_CAPACITY, enrolledCountOf(LOTTERY_OFFERING), "抽签后 enrolled_count 必须重算为在册数");
        assertEquals("2", jdbc().queryForObject("select round_status from tpm_selection_round where round_id = ?",
                String.class, ROUND_ID), "抽签完成后轮次必须置为已结束");

        List<String> before = lotterySnapshot();

        // 第二次：状态机必须拦住，且不得产生任何写入
        Map<String, Object> second = enrollmentService.runLottery(ROUND_ID, LOTTERY_SEED);
        assertTrue(String.valueOf(second.get("message")).contains("不允许抽签"),
                "已结束轮次必须被状态机挡住：" + second.get("message"));
        assertEquals(null, second.get("successCount"), "被挡回时不应返回统计结果");
        assertEquals(before, lotterySnapshot(), "重复抽签不得对已产生的中签/候补结果重新洗牌");
        assertEquals(LOTTERY_SEED, jdbc().queryForObject("select lottery_seed from tpm_selection_round where round_id = ?",
                Long.class, ROUND_ID).longValue(), "种子必须仍是首次抽签写入的值");
    }

    /** 抽签结果快照：按学生排序的「中签标记/结果状态/候补排名」三元组 */
    private List<String> lotterySnapshot()
    {
        return jdbc().queryForList("select concat(lottery_result, '|', result_status, '|', "
                + "ifnull(waitlist_rank, '-')) as snap from tpm_selection_enrollment "
                + "where round_id = ? and course_offering_id = ? order by student_id", String.class, ROUND_ID, LOTTERY_OFFERING);
    }
}
