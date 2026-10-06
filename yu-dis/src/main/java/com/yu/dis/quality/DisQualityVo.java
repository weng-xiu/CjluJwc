package com.yu.dis.quality;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据对接质量看板视图（N2 质量看板）。
 *
 * <p>汇总各同步任务的执行量、失败量与成功率，识别质量风险源，供主数据治理决策。
 *
 * @author N2
 */
public class DisQualityVo
{
    private Date generatedAt;
    private int taskCount;
    private long totalExecute;
    private long totalFail;
    /** 总体成功率（0~100，两位小数） */
    private double successRate;
    /** 失败率最高的任务（告警焦点） */
    private String worstTaskName;
    private double worstFailRate;
    private List<TaskQuality> tasks = new ArrayList<>();

    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
    public int getTaskCount() { return taskCount; }
    public void setTaskCount(int taskCount) { this.taskCount = taskCount; }
    public long getTotalExecute() { return totalExecute; }
    public void setTotalExecute(long totalExecute) { this.totalExecute = totalExecute; }
    public long getTotalFail() { return totalFail; }
    public void setTotalFail(long totalFail) { this.totalFail = totalFail; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public String getWorstTaskName() { return worstTaskName; }
    public void setWorstTaskName(String worstTaskName) { this.worstTaskName = worstTaskName; }
    public double getWorstFailRate() { return worstFailRate; }
    public void setWorstFailRate(double worstFailRate) { this.worstFailRate = worstFailRate; }
    public List<TaskQuality> getTasks() { return tasks; }
    public void setTasks(List<TaskQuality> tasks) { this.tasks = tasks; }

    /** 单个同步任务质量指标 */
    public static class TaskQuality
    {
        private Long taskId;
        private String taskName;
        private String taskCode;
        private long executeCount;
        private long failCount;
        private double failRate;
        private Date lastExecuteTime;

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getTaskCode() { return taskCode; }
        public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
        public long getExecuteCount() { return executeCount; }
        public void setExecuteCount(long executeCount) { this.executeCount = executeCount; }
        public long getFailCount() { return failCount; }
        public void setFailCount(long failCount) { this.failCount = failCount; }
        public double getFailRate() { return failRate; }
        public void setFailRate(double failRate) { this.failRate = failRate; }
        public Date getLastExecuteTime() { return lastExecuteTime; }
        public void setLastExecuteTime(Date lastExecuteTime) { this.lastExecuteTime = lastExecuteTime; }
    }
}
