package com.yu.sam.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 毕业论文环节留痕对象 sam_thesis_process
 *
 * <p>记录选题、开题、中期、查重、答辩、归档各环节的提交与审核过程，
 * 用于过程追溯与上级毕业论文抽检数据对接。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class SamThesisProcess extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long processId;

    /** 论文ID */
    private Long thesisId;

    /** 环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档） */
    @Excel(name = "环节", readConverterExp = "1=选题,2=开题,3=中期检查,4=查重,5=答辩,6=成绩归档")
    private String stage;

    /** 动作（submit提交 audit审核 record登记） */
    @Excel(name = "动作")
    private String action;

    /** 材料/环节名称 */
    @Excel(name = "材料名称")
    private String title;

    /** 提交内容或审核结论说明 */
    private String content;

    /** 附件地址 */
    private String attachment;

    /** 结果（0退回 1通过 2仅记录） */
    @Excel(name = "结果", readConverterExp = "0=退回,1=通过,2=仅记录")
    private String result;

    /** 本环节成绩 */
    @Excel(name = "环节成绩")
    private Double score;

    /** 意见 */
    private String opinion;

    /** 操作人登录名 */
    private String operator;

    /** 操作人姓名 */
    @Excel(name = "操作人")
    private String operatorName;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "操作时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    /** 论文题目（详情联表带出，便于待办列表展示） */
    private String topicName;

    /** 学生姓名（联表带出） */
    private String studentName;

    /** 学号（联表带出） */
    private String studentNo;

    public Long getProcessId() { return processId; }
    public void setProcessId(Long processId) { this.processId = processId; }

    public Long getThesisId() { return thesisId; }
    public void setThesisId(Long thesisId) { this.thesisId = thesisId; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAttachment() { return attachment; }
    public void setAttachment(String attachment) { this.attachment = attachment; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Date getOperateTime() { return operateTime; }
    public void setOperateTime(Date operateTime) { this.operateTime = operateTime; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("processId", getProcessId())
            .append("thesisId", getThesisId())
            .append("stage", getStage())
            .append("action", getAction())
            .append("title", getTitle())
            .append("result", getResult())
            .append("score", getScore())
            .append("operator", getOperator())
            .append("operateTime", getOperateTime())
            .append("remark", getRemark())
            .toString();
    }
}
