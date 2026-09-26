package com.yu.sam.domain;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 毕业论文选题库对象 sam_thesis_topic
 *
 * <p>毕业论文全过程管理第一个环节：教师申报题目、管理员审核上架、学生按容量选题。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class SamThesisTopic extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 题目ID */
    private Long topicId;

    /** 届别（如2026） */
    @Excel(name = "届别")
    private String planYear;

    /** 论文（设计）题目 */
    @Excel(name = "论文题目")
    private String topicName;

    /** 题目来源（0教师科研课题 1生产社会实践 2学生自拟 3学科竞赛） */
    @Excel(name = "题目来源", readConverterExp = "0=教师科研课题,1=生产社会实践,2=学生自拟,3=学科竞赛")
    private String topicSource;

    /** 适用专业ID */
    private Long majorId;

    /** 所属学院ID */
    private Long deptId;

    /** 指导教师登录名 */
    @Excel(name = "指导教师账号")
    private String advisor;

    /** 指导教师姓名 */
    @Excel(name = "指导教师")
    private String advisorName;

    /** 可选题人数 */
    @Excel(name = "可选题人数")
    private Integer capacity;

    /** 已选人数 */
    @Excel(name = "已选人数")
    private Integer electedCount;

    /** 难度（1基础 2中等 3较高） */
    @Excel(name = "难度", readConverterExp = "1=基础,2=中等,3=较高")
    private String difficulty;

    /** 题目简介与完成要求 */
    private String intro;

    /** 状态（0待审核 1可选题 2已选满 3已下架） */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=可选题,2=已选满,3=已下架")
    private String status;

    /** 审核意见 */
    private String auditOpinion;

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    @NotBlank(message = "届别不能为空")
    public String getPlanYear() { return planYear; }
    public void setPlanYear(String planYear) { this.planYear = planYear; }

    @NotBlank(message = "论文题目不能为空")
    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getTopicSource() { return topicSource; }
    public void setTopicSource(String topicSource) { this.topicSource = topicSource; }

    public Long getMajorId() { return majorId; }
    public void setMajorId(Long majorId) { this.majorId = majorId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getAdvisor() { return advisor; }
    public void setAdvisor(String advisor) { this.advisor = advisor; }

    public String getAdvisorName() { return advisorName; }
    public void setAdvisorName(String advisorName) { this.advisorName = advisorName; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getElectedCount() { return electedCount; }
    public void setElectedCount(Integer electedCount) { this.electedCount = electedCount; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAuditOpinion() { return auditOpinion; }
    public void setAuditOpinion(String auditOpinion) { this.auditOpinion = auditOpinion; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("topicId", getTopicId())
            .append("planYear", getPlanYear())
            .append("topicName", getTopicName())
            .append("topicSource", getTopicSource())
            .append("majorId", getMajorId())
            .append("deptId", getDeptId())
            .append("advisor", getAdvisor())
            .append("advisorName", getAdvisorName())
            .append("capacity", getCapacity())
            .append("electedCount", getElectedCount())
            .append("difficulty", getDifficulty())
            .append("intro", getIntro())
            .append("status", getStatus())
            .append("auditOpinion", getAuditOpinion())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
