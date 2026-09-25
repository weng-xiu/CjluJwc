package com.yu.sam.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 学业预警帮扶跟踪记录对象 sam_warning_assist_record（S6 帮扶闭环）
 * 每次帮扶沟通/干预登记一条记录，支撑帮扶过程可追溯。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
public class SamWarningAssistRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 帮扶任务ID */
    private Long assistId;

    /** 跟踪方式（0面谈 1电话 2线上 3家访） */
    private String followType;

    /** 跟踪内容 */
    private String followContent;

    /** 跟踪时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date followTime;

    /** 登记人用户ID */
    private Long operatorId;

    /** 登记人姓名 */
    private String operatorName;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public Long getAssistId() { return assistId; }
    public void setAssistId(Long assistId) { this.assistId = assistId; }

    public String getFollowType() { return followType; }
    public void setFollowType(String followType) { this.followType = followType; }

    public String getFollowContent() { return followContent; }
    public void setFollowContent(String followContent) { this.followContent = followContent; }

    public Date getFollowTime() { return followTime; }
    public void setFollowTime(Date followTime) { this.followTime = followTime; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("assistId", getAssistId())
            .append("followType", getFollowType())
            .append("followContent", getFollowContent())
            .append("followTime", getFollowTime())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
