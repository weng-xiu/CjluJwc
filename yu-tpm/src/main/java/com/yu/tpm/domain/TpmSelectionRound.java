package com.yu.tpm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

import java.util.Date;

/**
 * 选课轮次对象 tpm_selection_round
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public class TpmSelectionRound extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 轮次ID */
    private Long roundId;

    /** 学期ID */
    @Excel(name = "学期ID")
    private Long semesterId;

    /** 轮次名称 */
    @Excel(name = "轮次名称")
    private String roundName;

    /** 轮次顺序 */
    @Excel(name = "轮次顺序")
    private Integer roundOrder;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 每人最多选课门数 */
    @Excel(name = "最多选课门数")
    private Integer maxCoursesPerStudent;

    /** 轮次状态（0未开始 1进行中 2已结束） */
    @Excel(name = "轮次状态", readConverterExp = "0=未开始,1=进行中,2=已结束")
    private String roundStatus;

    /** 状态 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** T6：抽签随机种子（记录以支持结果复现审计） */
    @Excel(name = "抽签种子")
    private Long lotterySeed;

    /** T6：抽签执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "抽签时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lotteryTime;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 学期名称（关联查询，非持久化） */
    @Excel(name = "学期名称")
    private String semesterName;

    public Long getRoundId() { return roundId; }
    public void setRoundId(Long roundId) { this.roundId = roundId; }

    @NotNull(message = "学期ID不能为空")
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    @NotBlank(message = "轮次名称不能为空")
    @Size(min = 0, max = 100, message = "轮次名称长度不能超过100个字符")
    public String getRoundName() { return roundName; }
    public void setRoundName(String roundName) { this.roundName = roundName; }

    public Integer getRoundOrder() { return roundOrder; }
    public void setRoundOrder(Integer roundOrder) { this.roundOrder = roundOrder; }

    @NotNull(message = "开始时间不能为空")
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    @NotNull(message = "结束时间不能为空")
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }

    public Integer getMaxCoursesPerStudent() { return maxCoursesPerStudent; }
    public void setMaxCoursesPerStudent(Integer maxCoursesPerStudent) { this.maxCoursesPerStudent = maxCoursesPerStudent; }

    public String getRoundStatus() { return roundStatus; }
    public void setRoundStatus(String roundStatus) { this.roundStatus = roundStatus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getLotterySeed() { return lotterySeed; }
    public void setLotterySeed(Long lotterySeed) { this.lotterySeed = lotterySeed; }

    public Date getLotteryTime() { return lotteryTime; }
    public void setLotteryTime(Date lotteryTime) { this.lotteryTime = lotteryTime; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roundId", getRoundId())
            .append("semesterId", getSemesterId())
            .append("roundName", getRoundName())
            .append("roundOrder", getRoundOrder())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("maxCoursesPerStudent", getMaxCoursesPerStudent())
            .append("roundStatus", getRoundStatus())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
