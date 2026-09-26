package com.yu.sam.domain;

import java.util.Date;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 毕业论文（设计）全过程对象 sam_thesis
 *
 * <p>一名学生一届一条主记录，current_stage/stage_status 构成环节状态机：
 * 选题 → 开题 → 中期检查 → 查重 → 答辩 → 成绩归档。is_qualified 为学位审核
 * 论文分项的唯一数据来源（替代旧实现的恒置合格）。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class SamThesis extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 论文ID */
    private Long thesisId;

    /** 届别（如2026） */
    @Excel(name = "届别")
    private String planYear;

    /** 学生ID */
    @NotNull(message = "学生不能为空")
    private Long studentId;

    /** 学号（关联 sam_student 带出） */
    @Excel(name = "学号")
    private String studentNo;

    /** 学生姓名（关联 sam_student 带出） */
    @Excel(name = "学生姓名")
    private String studentName;

    /** 学院名称（关联带出） */
    @Excel(name = "学院")
    private String deptName;

    /** 专业名称（关联带出） */
    @Excel(name = "专业")
    private String majorName;

    /** 班级名称（关联带出） */
    @Excel(name = "班级")
    private String className;

    /** 选题库题目ID */
    private Long topicId;

    /** 论文题目快照 */
    @Excel(name = "论文题目")
    private String topicName;

    /** 指导教师登录名 */
    private String advisor;

    /** 指导教师姓名 */
    @Excel(name = "指导教师")
    private String advisorName;

    /** 当前环节（1选题 2开题 3中期检查 4查重 5答辩 6成绩归档） */
    @Excel(name = "当前环节", readConverterExp = "1=选题,2=开题,3=中期检查,4=查重,5=答辩,6=成绩归档")
    private String currentStage;

    /** 当前环节状态（0待提交 1待审核 2已通过 3已退回） */
    @Excel(name = "环节状态", readConverterExp = "0=待提交,1=待审核,2=已通过,3=已退回")
    private String stageStatus;

    /** 查重重复率（%） */
    @Excel(name = "查重率(%)")
    private Double checkRate;

    /** 查重是否达标（0否 1是） */
    @Excel(name = "查重达标", readConverterExp = "0=否,1=是")
    private String checkPass;

    /** 答辩成绩 */
    @Excel(name = "答辩成绩")
    private Double defenseScore;

    /** 总评成绩 */
    @Excel(name = "总评成绩")
    private Double totalScore;

    /** 成绩等级（0优秀 1良好 2中等 3及格 4不及格） */
    @Excel(name = "成绩等级", readConverterExp = "0=优秀,1=良好,2=中等,3=及格,4=不及格")
    private String gradeLevel;

    /** 论文是否合格（0否 1是） */
    @Excel(name = "是否合格", readConverterExp = "0=否,1=是")
    private String isQualified;

    /** 抽检状态（0未抽检 1已送抽检 2抽检合格 3抽检不合格） */
    @Excel(name = "抽检状态", readConverterExp = "0=未抽检,1=已送抽检,2=抽检合格,3=抽检不合格")
    private String sampleStatus;

    /** 成绩归档时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "归档时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date archiveTime;

    /** 记录状态（0正常 1作废） */
    private String status;

    /** 环节留痕明细（详情接口装配用，不参与列表查询） */
    private List<SamThesisProcess> processes;

    public Long getThesisId() { return thesisId; }
    public void setThesisId(Long thesisId) { this.thesisId = thesisId; }

    @NotBlank(message = "届别不能为空")
    public String getPlanYear() { return planYear; }
    public void setPlanYear(String planYear) { this.planYear = planYear; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }

    public String getTopicName() { return topicName; }
    public void setTopicName(String topicName) { this.topicName = topicName; }

    public String getAdvisor() { return advisor; }
    public void setAdvisor(String advisor) { this.advisor = advisor; }

    public String getAdvisorName() { return advisorName; }
    public void setAdvisorName(String advisorName) { this.advisorName = advisorName; }

    public String getCurrentStage() { return currentStage; }
    public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }

    public String getStageStatus() { return stageStatus; }
    public void setStageStatus(String stageStatus) { this.stageStatus = stageStatus; }

    public Double getCheckRate() { return checkRate; }
    public void setCheckRate(Double checkRate) { this.checkRate = checkRate; }

    public String getCheckPass() { return checkPass; }
    public void setCheckPass(String checkPass) { this.checkPass = checkPass; }

    public Double getDefenseScore() { return defenseScore; }
    public void setDefenseScore(Double defenseScore) { this.defenseScore = defenseScore; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }

    public String getIsQualified() { return isQualified; }
    public void setIsQualified(String isQualified) { this.isQualified = isQualified; }

    public String getSampleStatus() { return sampleStatus; }
    public void setSampleStatus(String sampleStatus) { this.sampleStatus = sampleStatus; }

    public Date getArchiveTime() { return archiveTime; }
    public void setArchiveTime(Date archiveTime) { this.archiveTime = archiveTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<SamThesisProcess> getProcesses() { return processes; }
    public void setProcesses(List<SamThesisProcess> processes) { this.processes = processes; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("thesisId", getThesisId())
            .append("planYear", getPlanYear())
            .append("studentId", getStudentId())
            .append("topicId", getTopicId())
            .append("topicName", getTopicName())
            .append("advisor", getAdvisor())
            .append("currentStage", getCurrentStage())
            .append("stageStatus", getStageStatus())
            .append("checkRate", getCheckRate())
            .append("checkPass", getCheckPass())
            .append("defenseScore", getDefenseScore())
            .append("totalScore", getTotalScore())
            .append("gradeLevel", getGradeLevel())
            .append("isQualified", getIsQualified())
            .append("sampleStatus", getSampleStatus())
            .append("archiveTime", getArchiveTime())
            .append("status", getStatus())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
