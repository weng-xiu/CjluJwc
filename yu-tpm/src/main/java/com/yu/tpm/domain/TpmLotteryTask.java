package com.yu.tpm.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 选课抽签任务对象 tpm_lottery_task
 *
 * <p>T-5：抽签从「HTTP 请求内同步执行」改为「任务表 + 状态机 + 进度查询」。
 * 任务行是执行轨迹与进度快照的唯一载体，抽签结果本身仍落在
 * {@code tpm_selection_enrollment} 与 {@code tpm_selection_round}。</p>
 *
 * <p>状态机：'0' 待执行 → '1' 执行中 → '2' 成功 / '3' 失败。
 * 迁移一律走带旧状态谓词的条件 update（见 {@code TpmLotteryTaskMapper}），
 * 因此重复执行、并发执行都不会出现「两个执行者都认为自己成功了」。</p>
 *
 * <p>幂等：{@code active_round} 是数据库生成列（'0'/'1' 时等于 round_id，其余为 NULL），
 * 配合唯一索引保证同一轮次同时只存在一个未完成任务。</p>
 *
 * @author ruoyi
 * @date 2026-11-01
 */
public class TpmLotteryTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 任务状态：待执行 */
    public static final String STATUS_PENDING = "0";

    /** 任务状态：执行中 */
    public static final String STATUS_RUNNING = "1";

    /** 任务状态：成功 */
    public static final String STATUS_SUCCEEDED = "2";

    /** 任务状态：失败 */
    public static final String STATUS_FAILED = "3";

    /** 抽签任务ID */
    private Long taskId;

    /** 选课轮次ID */
    @Excel(name = "轮次ID")
    private Long roundId;

    /** 任务状态（0待执行 1执行中 2成功 3失败） */
    @Excel(name = "任务状态", readConverterExp = "0=待执行,1=执行中,2=成功,3=失败")
    private String taskStatus;

    /** 本次抽签随机种子 */
    private Long lotterySeed;

    /** 参与抽签课程数（超容量需洗牌的课程数） */
    @Excel(name = "抽签课程数")
    private Integer lotteryCourses;

    /** 待抽签记录数（任务快照） */
    @Excel(name = "待抽签人数")
    private Integer totalCount;

    /** 已落库记录数（进度快照） */
    @Excel(name = "已处理人数")
    private Integer processedCount;

    /** 中签人数 */
    @Excel(name = "中签人数")
    private Integer admittedCount;

    /** 落选人数 */
    @Excel(name = "落选人数")
    private Integer rejectedCount;

    /** 失败原因（超长截断） */
    private String errorMsg;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间（成功或失败） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 轮次名称（关联展示字段，非本表列） */
    private String roundName;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getRoundId()
    {
        return roundId;
    }

    public void setRoundId(Long roundId)
    {
        this.roundId = roundId;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public Long getLotterySeed()
    {
        return lotterySeed;
    }

    public void setLotterySeed(Long lotterySeed)
    {
        this.lotterySeed = lotterySeed;
    }

    public Integer getLotteryCourses()
    {
        return lotteryCourses;
    }

    public void setLotteryCourses(Integer lotteryCourses)
    {
        this.lotteryCourses = lotteryCourses;
    }

    public Integer getTotalCount()
    {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount)
    {
        this.totalCount = totalCount;
    }

    public Integer getProcessedCount()
    {
        return processedCount;
    }

    public void setProcessedCount(Integer processedCount)
    {
        this.processedCount = processedCount;
    }

    public Integer getAdmittedCount()
    {
        return admittedCount;
    }

    public void setAdmittedCount(Integer admittedCount)
    {
        this.admittedCount = admittedCount;
    }

    public Integer getRejectedCount()
    {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount)
    {
        this.rejectedCount = rejectedCount;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getRoundName()
    {
        return roundName;
    }

    public void setRoundName(String roundName)
    {
        this.roundName = roundName;
    }

    /**
     * 进度百分比（0~100），前端直接展示用。
     * 失败/成功时按终态返回：成功 100；失败保留已完成的真实比例，便于判断卡在哪一步。
     * total 为 0（无待抽签记录）时返回 0，避免除零。
     */
    public int getProgressPercent()
    {
        if (STATUS_SUCCEEDED.equals(taskStatus))
        {
            return 100;
        }
        int total = totalCount == null ? 0 : totalCount;
        int processed = processedCount == null ? 0 : processedCount;
        if (total <= 0)
        {
            return 0;
        }
        if (processed >= total)
        {
            return 100;
        }
        return (int) (processed * 100L / total);
    }
}
