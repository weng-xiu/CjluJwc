package com.yu.tpm.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.tpm.domain.TpmLotteryTask;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.mapper.TpmLotteryTaskMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.tpm.service.ITpmLotteryTaskService;
import com.yu.tpm.task.LotteryTaskRunner;

/**
 * 选课抽签任务Service业务层处理（T-5）。
 *
 * @author ruoyi
 * @date 2026-11-01
 */
@Service
public class TpmLotteryTaskServiceImpl implements ITpmLotteryTaskService
{
    private static final Logger log = LoggerFactory.getLogger(TpmLotteryTaskServiceImpl.class);

    /** tpm_lottery_task.error_msg 是 varchar(500)，超长直接截断而不是让整条状态迁移失败 */
    private static final int ERROR_MSG_MAX_LENGTH = 500;

    @Autowired
    private TpmLotteryTaskMapper tpmLotteryTaskMapper;

    @Autowired
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    /**
     * 执行体用 {@link ObjectProvider} 懒解析：Runner 依赖本 Service 写状态，
     * 直接构造注入会形成双向依赖（Spring 在 3.x 之后默认禁止循环引用）。
     */
    @Autowired
    private ObjectProvider<LotteryTaskRunner> lotteryTaskRunner;

    @Override
    public TpmLotteryTask submitLotteryTask(Long roundId, Long seed)
    {
        if (roundId == null)
        {
            throw new ServiceException("轮次ID不能为空");
        }
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(roundId);
        if (round == null)
        {
            throw new ServiceException("选课轮次不存在");
        }
        // 与 runLottery 内的状态机闸门同源，这里提前挡一次是为了给提交方一个明确错误，而不是先落一条注定失败的任务
        if (!"1".equals(round.getRoundStatus()))
        {
            throw new ServiceException("轮次当前状态不允许抽签（仅进行中可抽签，已结束请先重置轮次状态）");
        }

        TpmLotteryTask task = new TpmLotteryTask();
        task.setRoundId(roundId);
        task.setTaskStatus(TpmLotteryTask.STATUS_PENDING);
        // 种子先随任务落库：这样“失败后重试”仍能复现同一次抽签，而不是重试就换了一套结果
        task.setLotterySeed(seed);
        task.setLotteryCourses(0);
        task.setTotalCount(0);
        task.setProcessedCount(0);
        task.setAdmittedCount(0);
        task.setRejectedCount(0);
        task.setCreateBy(currentUsername());
        task.setCreateTime(DateUtils.getNowDate());
        try
        {
            tpmLotteryTaskMapper.insertTpmLotteryTask(task);
        }
        catch (DuplicateKeyException e)
        {
            // uk_lottery_task_active：同一轮次已有「待执行/执行中」任务
            throw new ServiceException("该轮次已有抽签任务在执行中，请等待完成后再提交");
        }

        try
        {
            lotteryTaskRunner.getObject().executeAsync(task.getTaskId());
        }
        catch (Exception e)
        {
            // lotteryTaskExecutor 是 AbortPolicy：队列满会明确拒绝，而不是把抽签降级到 HTTP 线程里同步跑
            String reason = "抽签任务队列已满或线程池拒绝，请稍后重试";
            log.warn("抽签任务提交失败：taskId={}, roundId={}, cause={}", task.getTaskId(), roundId, e.getMessage());
            markFailed(task.getTaskId(), reason);
            task.setTaskStatus(TpmLotteryTask.STATUS_FAILED);
            task.setErrorMsg(reason);
            return task;
        }
        return task;
    }

    @Override
    public TpmLotteryTask selectTpmLotteryTaskByTaskId(Long taskId)
    {
        return tpmLotteryTaskMapper.selectTpmLotteryTaskByTaskId(taskId);
    }

    @Override
    public TpmLotteryTask selectLatestTaskByRoundId(Long roundId)
    {
        return tpmLotteryTaskMapper.selectLatestTaskByRoundId(roundId);
    }

    @Override
    public List<TpmLotteryTask> selectTpmLotteryTaskList(TpmLotteryTask tpmLotteryTask)
    {
        return tpmLotteryTaskMapper.selectTpmLotteryTaskList(tpmLotteryTask);
    }

    @Override
    public boolean markRunning(Long taskId)
    {
        return tpmLotteryTaskMapper.markRunning(taskId, DateUtils.getNowDate()) > 0;
    }

    @Override
    public void markSucceeded(TpmLotteryTask task)
    {
        task.setEndTime(task.getEndTime() == null ? DateUtils.getNowDate() : task.getEndTime());
        int rows = tpmLotteryTaskMapper.markSucceeded(task);
        if (rows == 0)
        {
            // 任务在抽签期间被改成失败（例如运维介入），不覆盖终态，只留痕
            log.warn("抽签任务结束态写回失败（任务已不是执行中）：taskId={}", task.getTaskId());
        }
    }

    @Override
    public void markFailed(Long taskId, String errorMsg)
    {
        if (taskId == null)
        {
            return;
        }
        tpmLotteryTaskMapper.markFailed(taskId, truncate(errorMsg), DateUtils.getNowDate());
    }

    /**
     * 进度快照写入。
     *
     * <p>{@code REQUIRES_NEW} 是关键：抽签整轮在单事务内跑，若这里与外层共用事务，
     * 进度就会一直“未提交”，外部查询看到的永远是 0——进度查询形同虚设。</p>
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void updateProgress(Long taskId, int processed, int total, int admitted, int rejected)
    {
        tpmLotteryTaskMapper.updateProgress(taskId, processed, total, admitted, rejected);
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            // 由定时任务/系统调用触发时没有登录上下文，不让“取用户名失败”挡住业务
            return "system";
        }
    }

    private String truncate(String text)
    {
        if (text == null)
        {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() <= ERROR_MSG_MAX_LENGTH ? trimmed : trimmed.substring(0, ERROR_MSG_MAX_LENGTH);
    }
}
