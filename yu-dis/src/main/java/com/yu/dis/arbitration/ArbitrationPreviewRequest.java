package com.yu.dis.arbitration;

import java.util.ArrayList;
import java.util.List;

/**
 * 试裁决请求（F5 主数据裁决前端化）。
 *
 * <p>前端在裁决矩阵页发起"试裁决"：给定同一业务主键（如学号/工号）的多源候选，
 * 后端按当前优先级配置裁决出权威版本并解释依据，不落库、纯预览。
 *
 * @author F5
 */
public class ArbitrationPreviewRequest
{
    /** 业务主键（如学号、工号） */
    private String businessKey;

    /** 多源候选 */
    private List<Candidate> candidates = new ArrayList<>();

    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }

    public List<Candidate> getCandidates() { return candidates; }
    public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

    /** 一条候选：来源系统编码 + 更新时间（字符串，按常见格式解析） + 可选载荷展示值。 */
    public static class Candidate
    {
        private String sourceCode;
        private String updateTime;
        private String value;

        public String getSourceCode() { return sourceCode; }
        public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

        public String getUpdateTime() { return updateTime; }
        public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
}
