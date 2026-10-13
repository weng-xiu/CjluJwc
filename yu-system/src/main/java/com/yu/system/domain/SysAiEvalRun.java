package com.yu.system.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 智能体发布前评测批次 sys_ai_eval_run（K4 AI 治理 · 发布前打分留痕主表）
 *
 * 一次评测（手动回归或发布门禁触发）落一行：记录生效阈值、通过用例数、通过率与最终门禁结果，
 * 逐条得分见 sys_ai_eval_result。result='1'（拦截）表示通过率低于阈值，发布动作应被阻止。
 *
 * @author yu
 * @date 2026-10-12
 */
public class SysAiEvalRun extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 触发方式：手动回归 */
    public static final String TRIGGER_MANUAL = "MANUAL";

    /** 触发方式：发布门禁 */
    public static final String TRIGGER_PUBLISH = "PUBLISH";

    /** 门禁结果：通过 */
    public static final String RESULT_PASS = "0";

    /** 门禁结果：拦截 */
    public static final String RESULT_BLOCK = "1";

    /** 批次ID */
    @Excel(name = "批次编号")
    private Long runId;

    /** 触发方式（MANUAL手动 PUBLISH发布门禁） */
    @Excel(name = "触发方式", readConverterExp = "MANUAL=手动回归,PUBLISH=发布门禁")
    private String triggerType;

    /** 本次评测所依据的模型版本（空表示本地抽取） */
    @Excel(name = "模型版本")
    private String modelVersion;

    /** 参与评测的用例总数 */
    @Excel(name = "用例总数")
    private Integer totalCases;

    /** 通过用例数 */
    @Excel(name = "通过数")
    private Integer passedCases;

    /** 通过率（百分比 0~100） */
    @Excel(name = "通过率(%)")
    private Double passRate;

    /** 平均检索置信度 */
    @Excel(name = "平均置信度")
    private Double avgConfidence;

    /** 本次生效的通过率阈值（百分比） */
    @Excel(name = "阈值(%)")
    private Double passThreshold;

    /** 门禁结果（0通过 1拦截） */
    @Excel(name = "结果", readConverterExp = "0=通过,1=拦截")
    private String result;

    /** 操作人 */
    @Excel(name = "操作人")
    private String operator;

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public String getTriggerType()
    {
        return triggerType;
    }

    public void setTriggerType(String triggerType)
    {
        this.triggerType = triggerType;
    }

    public String getModelVersion()
    {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion)
    {
        this.modelVersion = modelVersion;
    }

    public Integer getTotalCases()
    {
        return totalCases;
    }

    public void setTotalCases(Integer totalCases)
    {
        this.totalCases = totalCases;
    }

    public Integer getPassedCases()
    {
        return passedCases;
    }

    public void setPassedCases(Integer passedCases)
    {
        this.passedCases = passedCases;
    }

    public Double getPassRate()
    {
        return passRate;
    }

    public void setPassRate(Double passRate)
    {
        this.passRate = passRate;
    }

    public Double getAvgConfidence()
    {
        return avgConfidence;
    }

    public void setAvgConfidence(Double avgConfidence)
    {
        this.avgConfidence = avgConfidence;
    }

    public Double getPassThreshold()
    {
        return passThreshold;
    }

    public void setPassThreshold(Double passThreshold)
    {
        this.passThreshold = passThreshold;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator(String operator)
    {
        this.operator = operator;
    }
}
