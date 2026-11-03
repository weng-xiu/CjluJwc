package com.yu.tpm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.AjaxResult;
import com.yu.framework.cache.DistributedLock;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.service.ITpmLotteryTaskService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * 选课名单接口层测试（Q1 第三批：MockMvc standalone）。
 * 校验 HTTP 路由、@Validated 请求体校验、PathVariable/RequestParam 绑定与抽签 seed 透传契约；
 * 抽签/递补/冲突判定等业务算法由 TpmSelectionEnrollmentServiceImplTest 纯单测覆盖。
 *
 * <p>T-5 契约变更：{@code POST /lottery/{roundId}} 不再同步返回中签统计，而是落任务行并返回 taskId，
 * 进度与结果改走 {@code GET /lottery/task/{taskId}} 与 {@code /lottery/task/round/{roundId}}。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmSelectionEnrollmentControllerMockMvcTest {

    @Mock
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    /** A3：抽签/递补写路径依赖分布式锁，standalone 需显式提供 mock，否则控制器字段为 null */
    @Mock
    private DistributedLock distributedLock;

    /** T-5：抽签改为异步任务，控制器依赖任务 Service */
    @Mock
    private ITpmLotteryTaskService tpmLotteryTaskService;

    @InjectMocks
    private TpmSelectionEnrollmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        // 默认令递补获锁成功，进入业务分支
        when(distributedLock.tryAcquire(any(String.class), anyLong(), anyLong())).thenReturn(null);
    }

    private TpmLotteryTask task(String status)
    {
        TpmLotteryTask task = new TpmLotteryTask();
        task.setTaskId(88L);
        task.setRoundId(3L);
        task.setTaskStatus(status);
        return task;
    }

    @Test
    @DisplayName("getInfo：PathVariable绑定并返回200与数据体")
    void getInfo_returnsEnrollment() throws Exception {
        TpmSelectionEnrollment e = new TpmSelectionEnrollment();
        e.setEnrollId(1L);
        e.setStudentId(10L);
        e.setCourseOfferingId(20L);
        when(tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentByEnrollId(eq(1L))).thenReturn(e);

        mockMvc.perform(get("/tpm/enroll/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enrollId").value(1))
                .andExpect(jsonPath("$.data.studentId").value(10));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated校验并落库")
    void add_validBodyPersists() throws Exception {
        when(tpmSelectionEnrollmentService.insertTpmSelectionEnrollment(
                org.mockito.ArgumentMatchers.any(TpmSelectionEnrollment.class))).thenReturn(1);

        String body = "{\"roundId\":1,\"studentId\":10,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TpmSelectionEnrollment> captor = ArgumentCaptor.forClass(TpmSelectionEnrollment.class);
        verify(tpmSelectionEnrollmentService).insertTpmSelectionEnrollment(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(10L), captor.getValue().getStudentId());
    }

    @Test
    @DisplayName("add：缺失必填studentId被@NotNull拦截返回400，不落库")
    void add_missingRequiredFieldRejected() throws Exception {
        String body = "{\"roundId\":1,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(tpmSelectionEnrollmentService, never())
                .insertTpmSelectionEnrollment(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("validate：冲突检测端点透传三元组并返回警告列表")
    void validateSelection_passesParams() throws Exception {
        when(tpmSelectionEnrollmentService.checkSelectionConflicts(eq(10L), eq(20L), eq(1L)))
                .thenReturn(Collections.emptyList());

        String body = "{\"roundId\":1,\"studentId\":10,\"courseOfferingId\":20}";
        mockMvc.perform(post("/tpm/enroll/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("lottery：不再同步执行，只落任务行并返回 taskId（传入seed原样透传）")
    void lottery_submitsTaskWithSeed() throws Exception {
        when(tpmLotteryTaskService.submitLotteryTask(eq(3L), eq(99L))).thenReturn(task(TpmLotteryTask.STATUS_PENDING));

        mockMvc.perform(post("/tpm/enroll/lottery/3").param("seed", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(88))
                .andExpect(jsonPath("$.data.taskStatus").value(TpmLotteryTask.STATUS_PENDING));

        verify(tpmLotteryTaskService).submitLotteryTask(eq(3L), eq(99L));
        // 关键契约：请求线程绝不触碰抽签业务，否则又退化成同步长请求
        verify(tpmSelectionEnrollmentService, never()).runLottery(anyLong(), any(), any());
    }

    @Test
    @DisplayName("lottery：不传seed时以null提交（执行阶段自动生成并记录）")
    void lottery_submitsTaskWithoutSeed() throws Exception {
        when(tpmLotteryTaskService.submitLotteryTask(eq(3L), isNull())).thenReturn(task(TpmLotteryTask.STATUS_PENDING));

        mockMvc.perform(post("/tpm/enroll/lottery/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("抽签任务已提交")));

        verify(tpmLotteryTaskService).submitLotteryTask(eq(3L), isNull());
    }

    @Test
    @DisplayName("lottery：提交即失败（线程池拒绝）时返回错误码并透出原因，不让前端误以为已排队")
    void lottery_rejectedReturnsError() throws Exception {
        TpmLotteryTask failed = task(TpmLotteryTask.STATUS_FAILED);
        failed.setErrorMsg("抽签任务队列已满，请稍后重试");
        when(tpmLotteryTaskService.submitLotteryTask(eq(3L), isNull())).thenReturn(failed);

        mockMvc.perform(post("/tpm/enroll/lottery/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("抽签任务队列已满，请稍后重试"));
    }

    @Test
    @DisplayName("lotteryTask：按任务号查询进度")
    void lotteryTask_returnsProgress() throws Exception {
        TpmLotteryTask running = task(TpmLotteryTask.STATUS_RUNNING);
        running.setTotalCount(1200);
        running.setProcessedCount(500);
        when(tpmLotteryTaskService.selectTpmLotteryTaskByTaskId(eq(88L))).thenReturn(running);

        mockMvc.perform(get("/tpm/enroll/lottery/task/88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value(TpmLotteryTask.STATUS_RUNNING))
                .andExpect(jsonPath("$.data.processedCount").value(500));
    }

    @Test
    @DisplayName("lotteryTask：任务号不存在时返回可读错误")
    void lotteryTask_notFound() throws Exception {
        when(tpmLotteryTaskService.selectTpmLotteryTaskByTaskId(eq(404L))).thenReturn(null);

        mockMvc.perform(get("/tpm/enroll/lottery/task/404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("抽签任务不存在"));
    }

    @Test
    @DisplayName("latestLotteryTask：从未抽过签时返回 data=null，供前端判断是否续接进度")
    void latestLotteryTask_nullWhenNeverRun() throws Exception {
        when(tpmLotteryTaskService.selectLatestTaskByRoundId(eq(3L))).thenReturn(null);

        mockMvc.perform(get("/tpm/enroll/lottery/task/round/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("promoteWaitlist：拿不到分布式锁时返回忙提示，不进入业务")
    void promoteWaitlist_lockBusy() throws Exception {
        mockMvc.perform(post("/tpm/enroll/promoteWaitlist/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该开课递补正在进行中，请稍后重试"));

        verify(tpmSelectionEnrollmentService, never()).promoteWaitlist(anyLong());
    }

    @Test
    @DisplayName("drop：退课端点直接返回服务层AjaxResult契约")
    void drop_returnsServiceResult() throws Exception {
        when(tpmSelectionEnrollmentService.dropCourse(eq(7L)))
                .thenReturn(AjaxResult.success("退课成功"));

        mockMvc.perform(post("/tpm/enroll/drop/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("退课成功"));
    }
}
