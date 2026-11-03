package com.yu.tpm.task;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.yu.framework.cache.DistributedLock;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.service.ITpmLotteryTaskService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * 抽签任务执行体（T-5）。
 *
 * <p>一次执行分四步：</p>
 * <ol>
 *   <li><b>抢状态</b>：'0' 待执行 → '1' 执行中，条件更新，抢不到说明别人已在跑或已结束；</li>
 *   <li><b>拿锁</b>：轮次级分布式锁，{@link DistributedLock#tryAcquire} 带看门狗续期——租约固定 30 秒，
 *       但任务跑多久就续多久，不再用“把 TTL 估得足够长”来赌任务时长（T-5 的另一半）；</li>
 *   <li><b>抽签</b>：调用同步实现并注入进度回调，回调写任务表（独立事务）；</li>
 *   <li><b>落终态</b>：成功落种子与统计，异常落失败原因（超长截断）。</li>
 * </ol>
 *
 * <p>为什么状态机与分布式锁都要：状态机防同库重复执行（持久、不可绕过），
 * 锁防跨轮次/跨实例的资源争用（recountEnrolled 是全量重算，多跑一次就会与在途写互相覆盖）。
 * 两者任一失效都还剩另一道。</p>
 *
 * @author ruoyi
 */
@Component
public class LotteryTaskRunner
{
    private static final Logger log = LoggerFactory.getLogger(LotteryTaskRunner.class);

    /**
     * 抽签锁单次租约（毫秒）。这个值不再是“任务能跑多久”的上限：
     * 句柄在存活期间每租约的 1/3 自动续期，任务结束才释放。
     */
    private static final long LEASE_TIME_MS = 30000L;

    @Autowired
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    @Autowired
    private ITpmLotteryTaskService tpmLotteryTaskService;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    @Qualifier("lotteryTaskExecutor")
    private ThreadPoolTaskExecutor lotteryTaskExecutor;

    /**
     * 提交到专用线程池。队列满时抛异常，由调用方把任务置为失败，
     * 绝不回落到调用线程同步执行（那会把异步悄悄变回同步）。
     */
    public void executeAsync(Long taskId)
    {
        lotteryTaskExecutor.execute(() -> execute(taskId));
    }

    /**
     * 同步执行一个已落库的任务（线程池 worker 与单测共用入口）。
     *
     * <p>本方法自行捕获全部异常：任务表的终态必须落盘，否则前端会永远看到“执行中”。</p>
     */
    public void execute(Long taskId)
    {
        if (!tpmLotteryTaskService.markRunning(taskId))
        {
            log.info("抽签任务已被其他执行者接手或已结束，跳过：taskId={}", taskId);
            return;
        }
        TpmLotteryTask task = tpmLotteryTaskService.selectTpmLotteryTaskByTaskId(taskId);
        if (task == null)
        {
            log.warn("抽签任务不存在（可能已被删除）：taskId={}", taskId);
            return;
        }

        String lockKey = "selection:lottery:" + task.getRoundId();
        try (DistributedLock.LockHandle handle = distributedLock.tryAcquire(lockKey, 0L, LEASE_TIME_MS))
        {
            if (handle == null)
            {
                tpmLotteryTaskService.markFailed(taskId, "该轮次已有抽签正在执行（分布式锁未获取），请稍后重试");
                return;
            }
            Map<String, Object> result = runWithProgress(task);
            if (!Boolean.TRUE.equals(result.get("success")))
            {
                // 轮次不存在 / 状态被改回未开始：属于业务拒绝，不是系统异常，原因直接落任务表
                tpmLotteryTaskService.markFailed(taskId, String.valueOf(result.get("message")));
                return;
            }
            tpmLotteryTaskService.markSucceeded(toSucceededTask(task, result));
        }
        catch (Exception e)
        {
            log.error("抽签任务执行失败：taskId={}, roundId={}", taskId, task.getRoundId(), e);
            tpmLotteryTaskService.markFailed(taskId, describe(e));
        }
    }

    /** 抽签 + 进度回写；返回实现层的结果统计 */
    private Map<String, Object> runWithProgress(TpmLotteryTask task)
    {
        final Long taskId = task.getTaskId();
        return tpmSelectionEnrollmentService.runLottery(task.getRoundId(), task.getLotterySeed(),
                (processed, total, admitted, rejected) ->
                        tpmLotteryTaskService.updateProgress(taskId, processed, total, admitted, rejected));
    }

    /** 把抽签结果统计整理成任务终态行（只填需要落库的列） */
    private TpmLotteryTask toSucceededTask(TpmLotteryTask executed, Map<String, Object> result)
    {
        TpmLotteryTask done = new TpmLotteryTask();
        done.setTaskId(executed.getTaskId());
        done.setLotteryCourses(intOf(result.get("lotteryCourses")));
        done.setAdmittedCount(intOf(result.get("successCount")));
        done.setRejectedCount(intOf(result.get("failCount")));
        // 实际使用的种子（提交时没传则实现层生成），落库后可复现审计
        done.setLotterySeed(longOf(result.get("seed"), executed.getLotterySeed()));
        int total = intOf(result.get("successCount")) + intOf(result.get("failCount"));
        done.setTotalCount(total);
        done.setProcessedCount(total);
        return done;
    }

    private int intOf(Object value)
    {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private Long longOf(Object value, Long fallback)
    {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private String describe(Exception e)
    {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
