package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.RejectedExecutionException;

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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.exception.ServiceException;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.mapper.TpmLotteryTaskMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.tpm.task.LotteryTaskRunner;

/**
 * T-5：抽签任务 Service 单元测试。
 *
 * <p>覆盖三件必须成立的事：① 同一轮次不许有第二个未完成任务（唯一索引 + 明确错误）；
 * ② 线程池拒绝时任务必须落失败态而不是“看起来提交成功”；③ 进度写入必须 REQUIRES_NEW——
 * 若退化成 REQUIRED，进度会跟着抽签事务一起待在未提交状态，外部查询永远是 0。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TpmLotteryTaskServiceImplTest
{
    @Mock
    private TpmLotteryTaskMapper tpmLotteryTaskMapper;

    @Mock
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    @Mock
    private ObjectProvider<LotteryTaskRunner> lotteryTaskRunnerProvider;

    @Mock
    private LotteryTaskRunner lotteryTaskRunner;

    @InjectMocks
    private TpmLotteryTaskServiceImpl service;

    @BeforeEach
    void setUp()
    {
        when(lotteryTaskRunnerProvider.getObject()).thenReturn(lotteryTaskRunner);
        // 插入成功时返回 1，并让 MyBatis 风格的 useGeneratedKeys 行为可断言
        lenient().when(tpmLotteryTaskMapper.insertTpmLotteryTask(any(TpmLotteryTask.class))).thenAnswer(invocation -> {
            TpmLotteryTask task = invocation.getArgument(0);
            task.setTaskId(88L);
            return 1;
        });
    }

    private TpmSelectionRound round(String status)
    {
        TpmSelectionRound round = new TpmSelectionRound();
        round.setRoundId(1L);
        round.setRoundStatus(status);
        return round;
    }

    @Test
    @DisplayName("提交：轮次不存在直接拒绝，不落任务行")
    void submit_roundNotFound()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(9L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submitLotteryTask(9L, null));
        assertTrue(ex.getMessage().contains("轮次不存在"));
        verify(tpmLotteryTaskMapper, never()).insertTpmLotteryTask(any());
    }

    @Test
    @DisplayName("提交：轮次非进行中直接拒绝（与 runLottery 内的状态机闸门同源）")
    void submit_roundNotOpen()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round("2"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submitLotteryTask(1L, null));
        assertTrue(ex.getMessage().contains("不允许抽签"));
        verify(tpmLotteryTaskMapper, never()).insertTpmLotteryTask(any());
    }

    @Test
    @DisplayName("提交：同轮次已有未完成任务时被唯一索引挡住，返回可读错误")
    void submit_activeTaskExists()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round("1"));
        when(tpmLotteryTaskMapper.insertTpmLotteryTask(any(TpmLotteryTask.class)))
                .thenThrow(new DuplicateKeyException("uk_lottery_task_active"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submitLotteryTask(1L, null));
        assertTrue(ex.getMessage().contains("已有抽签任务在执行中"), ex.getMessage());
        verify(lotteryTaskRunner, never()).executeAsync(anyLong());
    }

    @Test
    @DisplayName("提交：落「待执行」任务行并把 taskId 交给执行体，种子随任务一起落库")
    void submit_success_enqueuesTask()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round("1"));

        TpmLotteryTask task = service.submitLotteryTask(1L, 4242L);

        ArgumentCaptor<TpmLotteryTask> captor = ArgumentCaptor.forClass(TpmLotteryTask.class);
        verify(tpmLotteryTaskMapper).insertTpmLotteryTask(captor.capture());
        assertEquals(TpmLotteryTask.STATUS_PENDING, captor.getValue().getTaskStatus());
        assertEquals(Long.valueOf(4242L), captor.getValue().getLotterySeed(), "种子必须随任务落库，失败重试才能复现同一结果");
        assertEquals(Long.valueOf(88L), task.getTaskId());
        verify(lotteryTaskRunner).executeAsync(88L);
    }

    @Test
    @DisplayName("提交：线程池拒绝时任务就地置失败，绝不回落到调用线程同步执行")
    void submit_rejected_marksFailed()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(1L)).thenReturn(round("1"));
        doThrow(new RejectedExecutionException("queue full")).when(lotteryTaskRunner).executeAsync(anyLong());

        TpmLotteryTask task = service.submitLotteryTask(1L, null);

        verify(tpmLotteryTaskMapper).markFailed(eq(88L), anyString(), any());
        assertEquals(TpmLotteryTask.STATUS_FAILED, task.getTaskStatus());
        assertTrue(task.getErrorMsg().contains("队列已满"), task.getErrorMsg());
        // 拒绝路径不得调用业务抽签
        verify(tpmLotteryTaskMapper, never()).markSucceeded(any());
    }

    @Test
    @DisplayName("状态机：markRunning 以 affected rows 判定是否抢到任务")
    void markRunning_basedOnAffectedRows()
    {
        when(tpmLotteryTaskMapper.markRunning(eq(1L), any())).thenReturn(1);
        assertTrue(service.markRunning(1L));

        when(tpmLotteryTaskMapper.markRunning(eq(2L), any())).thenReturn(0);
        assertFalse(service.markRunning(2L), "affected=0 说明任务已被他人接手");
    }

    @Test
    @DisplayName("失败原因超长时截断到列宽，不让整条状态迁移失败")
    void markFailed_truncatesLongReason()
    {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 900; i++)
        {
            longText.append('x');
        }

        service.markFailed(1L, longText.toString());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(tpmLotteryTaskMapper).markFailed(eq(1L), captor.capture(), any());
        assertEquals(500, captor.getValue().length());
    }

    @Test
    @DisplayName("进度写入必须 REQUIRES_NEW（否则进度跟着抽签事务一起不可见）")
    void updateProgress_requiresNewTransaction() throws Exception
    {
        Method method = TpmLotteryTaskServiceImpl.class
                .getMethod("updateProgress", Long.class, int.class, int.class, int.class, int.class);
        Transactional tx = method.getAnnotation(Transactional.class);

        assertTrue(tx != null, "updateProgress 必须声明 @Transactional");
        assertEquals(Propagation.REQUIRES_NEW, tx.propagation(),
                "抽签在单事务内跑，进度必须独立提交，否则外部查询永远是 0");
        assertTrue(containsRollbackFor(tx), "T-2 门禁：必须声明 rollbackFor");
    }

    private boolean containsRollbackFor(Transactional tx)
    {
        for (Class<? extends Throwable> type : tx.rollbackFor())
        {
            if (Exception.class.equals(type))
            {
                return true;
            }
        }
        return false;
    }
}
