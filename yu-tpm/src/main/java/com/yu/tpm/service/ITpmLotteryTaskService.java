package com.yu.tpm.service;

import java.util.List;

import com.yu.tpm.domain.TpmLotteryTask;

/**
 * 选课抽签任务Service接口（T-5）。
 *
 * @author ruoyi
 * @date 2026-11-01
 */
public interface ITpmLotteryTaskService
{
    /**
     * 提交抽签任务并立即返回（异步执行）。
     *
     * <p>只做三件事：校验轮次状态、落「待执行」任务行（同一轮次有未完成任务时被唯一索引挡住）、
     * 交给专用线程池。真正的抽签在 {@code LotteryTaskRunner} 中执行。</p>
     *
     * @param roundId 轮次ID
     * @param seed    可选随机种子（为空时执行阶段自动生成）
     * @return 刚创建的任务行（含 taskId）
     */
    TpmLotteryTask submitLotteryTask(Long roundId, Long seed);

    /** 按主键查询任务（进度查询） */
    TpmLotteryTask selectTpmLotteryTaskByTaskId(Long taskId);

    /** 查询某轮次最近一次任务 */
    TpmLotteryTask selectLatestTaskByRoundId(Long roundId);

    /** 条件查询任务列表 */
    List<TpmLotteryTask> selectTpmLotteryTaskList(TpmLotteryTask tpmLotteryTask);

    /**
     * '0' 待执行 → '1' 执行中。
     *
     * @return true=本执行者抢到任务；false=已被其他执行者接手或任务已结束
     */
    boolean markRunning(Long taskId);

    /** '1' 执行中 → '2' 成功（落盘种子与统计） */
    void markSucceeded(TpmLotteryTask task);

    /** '0'/'1' → '3' 失败（原因超长截断，不覆盖已完成的进度） */
    void markFailed(Long taskId, String errorMsg);

    /**
     * 推进进度快照。**独立事务**（REQUIRES_NEW）：调用发生在抽签事务内部，
     * 与外层共用连接会让外部查询看不到未提交的进度。
     */
    void updateProgress(Long taskId, int processed, int total, int admitted, int rejected);
}
