package com.yu.tpm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.yu.tpm.domain.TpmLotteryTask;

/**
 * 选课抽签任务Mapper接口
 *
 * <p>状态迁移一律是「带旧状态谓词的条件 update」，调用方以 affected rows 判定是否切换成功，
 * 不做 select-then-write（否则两个执行者都会认为自己拿到了任务）。</p>
 *
 * @author ruoyi
 * @date 2026-11-01
 */
public interface TpmLotteryTaskMapper
{
    /** 按主键查询任务 */
    TpmLotteryTask selectTpmLotteryTaskByTaskId(Long taskId);

    /** 查询某轮次最近一次任务（进度查询默认入口） */
    TpmLotteryTask selectLatestTaskByRoundId(Long roundId);

    /** 条件查询任务列表 */
    List<TpmLotteryTask> selectTpmLotteryTaskList(TpmLotteryTask tpmLotteryTask);

    /**
     * 插入任务。同一轮次已有「待执行/执行中」任务时，
     * 唯一索引 uk_lottery_task_active 会抛出 DuplicateKeyException。
     */
    int insertTpmLotteryTask(TpmLotteryTask tpmLotteryTask);

    /** '0' 待执行 → '1' 执行中；affected=0 说明任务已被他人接手或已结束 */
    int markRunning(@Param("taskId") Long taskId, @Param("startTime") Date startTime);

    /**
     * 执行中推进进度快照（total 在抽签开始时已确定，一并写入）；
     * affected=0 说明任务已不是执行中（被取消/已结束）。
     */
    int updateProgress(@Param("taskId") Long taskId,
                       @Param("processedCount") Integer processedCount,
                       @Param("totalCount") Integer totalCount,
                       @Param("admittedCount") Integer admittedCount,
                       @Param("rejectedCount") Integer rejectedCount);

    /** '1' 执行中 → '2' 成功，同时落盘种子与统计 */
    int markSucceeded(TpmLotteryTask tpmLotteryTask);

    /** '0'/'1' → '3' 失败（含提交即失败：线程池拒绝） */
    int markFailed(@Param("taskId") Long taskId,
                   @Param("errorMsg") String errorMsg,
                   @Param("endTime") Date endTime);

    /** 逻辑删除 */
    int deleteTpmLotteryTaskByTaskId(Long taskId);
}
