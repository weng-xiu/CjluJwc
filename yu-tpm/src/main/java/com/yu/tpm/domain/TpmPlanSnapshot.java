package com.yu.tpm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 人才培养方案版本快照对象 tpm_plan_snapshot
 *
 * <p>F2 方案版本快照：方案每次发布生成一条不可变快照，记录实质内容指纹与
 * 学分结构/课程清单的发布时刻定格 JSON，支撑"按指定历史版本比对"（毕业审核可追溯）。
 *
 * @author F2
 * @date 2026-10-17
 */
public class TpmPlanSnapshot extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 快照ID */
    private Long snapshotId;

    /** 方案ID */
    private Long planId;

    /** 发布时的版本号 */
    private String version;

    /** 实质内容指纹（SHA-256，十六进制小写） */
    private String contentHash;

    /** 快照时的要求总学分 */
    private Double totalCredit;

    /** 学分结构定格快照JSON */
    private String creditStructureJson;

    /** 课程清单定格快照JSON */
    private String courseListJson;

    /** 方案发布日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date publishDate;

    /** 快照生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public Double getTotalCredit() { return totalCredit; }
    public void setTotalCredit(Double totalCredit) { this.totalCredit = totalCredit; }

    public String getCreditStructureJson() { return creditStructureJson; }
    public void setCreditStructureJson(String creditStructureJson) { this.creditStructureJson = creditStructureJson; }

    public String getCourseListJson() { return courseListJson; }
    public void setCourseListJson(String courseListJson) { this.courseListJson = courseListJson; }

    public Date getPublishDate() { return publishDate; }
    public void setPublishDate(Date publishDate) { this.publishDate = publishDate; }

    public Date getSnapshotTime() { return snapshotTime; }
    public void setSnapshotTime(Date snapshotTime) { this.snapshotTime = snapshotTime; }
}
