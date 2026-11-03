package com.yu.tpm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.yu.framework.cache.DistributedLock;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.service.ITpmLotteryTaskService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.tpm.service.LotteryProgressListener;

/**
 * T-5：抽签任务执行体单元测试。
 *
 * <p>核心是「终态一定会落盘」：抢不到状态、拿不到锁、业务软失败、抛异常四条路径
 * 都必须让任务表离开 '1' 执行中，否则前端会永久转圈。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LotteryTaskRunnerTest
{
    @Mock
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    @Mock
    private ITpmLotteryTaskService tpmLotteryTaskService;

    @Mock
    private DistributedLock distributedLock;

    @Mock
    private DistributedLock.LockHandle lockHandle;

    @Mock
    private ThreadPoolTaskExecutor lotteryTaskExecutor;

    @InjectMocks
    private LotteryTaskRunner runner;

    private TpmLotteryTask pending;

    @BeforeEach
    void setUp()
    {
        pending = new TpmLotteryTask();
        pending.setTaskId(7L);
        pending.setRoundId(1L);
        pending.setTaskStatus(TpmLotteryTask.STATUS_PENDING);
        pending.setLotterySeed(4242L);

        when(tpmLotteryTaskService.markRunning(7L)).thenReturn(true);
        when(tpmLotteryTaskService.selectTpmLotteryTaskByTaskId(7L)).thenReturn(pending);
        when(distributedLock.tryAcquire(anyString(), anyLong(), anyLong())).thenReturn(lockHandle);
    }

    private Map<String, Object> successResult()
    {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("lotteryCourses", 3);
        result.put("successCount", 900);
        result.put("failCount", 300);
        result.put("seed", 4242L);
        return result;
    }

    @Test
    @DisplayName("抢不到状态：直接跳过，不重复执行抽签")
    void execute_skipsWhenNotOwner()
    {
        when(tpmLotteryTaskService.markRunning(7L)).thenReturn(false);

        runner.execute(7L);

        verify(tpmSelectionEnrollmentService, never()).runLottery(anyLong(), any(), any());
        verify(distributedLock, never()).tryAcquire(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("拿不到分布式锁：任务置失败并说明原因（另一实例正在跑）")
    void execute_marksFailedWhenLockMissing()
    {
        when(distributedLock.tryAcquire(anyString(), anyLong(), anyLong())).thenReturn(null);

        runner.execute(7L);

        verify(tpmLotteryTaskService).markFailed(eq(7L), anyString());
        verify(tpmSelectionEnrollmentService, never()).runLottery(anyLong(), any(), any());
        verify(tpmLotteryTaskService, never()).markSucceeded(any());
    }

    @Test
    @DisplayName("成功执行：落种子与统计，并把进度回调接到任务表")
    void execute_success_recordsStats()
    {
        when(tpmSelectionEnrollmentService.runLottery(anyLong(), any(), any())).thenReturn(successResult());

        runner.execute(7L);

        ArgumentCaptor<LotteryProgressListener> listener = ArgumentCaptor.forClass(LotteryProgressListener.class);
        verify(tpmSelectionEnrollmentService).runLottery(eq(1L), eq(4242L), listener.capture());
        // 回调必须落到任务表的独立事务写入方法上
        listener.getValue().onProgress(500, 1200, 900, 300);
        verify(tpmLotteryTaskService).updateProgress(eq(7L), eq(500), eq(1200), eq(900), eq(300));

        ArgumentCaptor<TpmLotteryTask> done = ArgumentCaptor.forClass(TpmLotteryTask.class);
        verify(tpmLotteryTaskService).markSucceeded(done.capture());
        assertEquals(Long.valueOf(7L), done.getValue().getTaskId());
        assertEquals(Integer.valueOf(3), done.getValue().getLotteryCourses());
        assertEquals(Integer.valueOf(900), done.getValue().getAdmittedCount());
        assertEquals(Integer.valueOf(300), done.getValue().getRejectedCount());
        assertEquals(Long.valueOf(4242L), done.getValue().getLotterySeed());
        assertEquals(Integer.valueOf(1200), done.getValue().getTotalCount());
        verify(tpmLotteryTaskService, never()).markFailed(anyLong(), anyString());
    }

    @Test
    @DisplayName("业务软失败（轮次不存在/状态被改）：落失败态并带上实现层给出的原因")
    void execute_softFailure_marksFailed()
    {
        Map<String, Object> softFail = new HashMap<>();
        softFail.put("success", false);
        softFail.put("message", "轮次当前状态不允许抽签（仅进行中可抽签，已结束请先重置轮次状态）");
        when(tpmSelectionEnrollmentService.runLottery(anyLong(), any(), any())).thenReturn(softFail);

        runner.execute(7L);

        verify(tpmLotteryTaskService).markFailed(7L, "轮次当前状态不允许抽签（仅进行中可抽签，已结束请先重置轮次状态）");
        verify(tpmLotteryTaskService, never()).markSucceeded(any());
    }

    @Test
    @DisplayName("执行抛异常：任务置失败并写入异常信息，不把异常抛出线程池")
    void execute_exception_marksFailed()
    {
        when(tpmSelectionEnrollmentService.runLottery(anyLong(), any(), any()))
                .thenThrow(new IllegalStateException("connection reset"));

        runner.execute(7L);

        verify(tpmLotteryTaskService).markFailed(7L, "connection reset");
        verify(tpmLotteryTaskService, never()).markSucceeded(any());
    }

    @Test
    @DisplayName("异步提交：交给专用抽签线程池，不在调用线程执行")
    void executeAsync_submitsToDedicatedPool()
    {
        runner.executeAsync(7L);

        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(lotteryTaskExecutor).execute(task.capture());
        verify(tpmLotteryTaskService, never()).markRunning(anyLong());
    }
}
