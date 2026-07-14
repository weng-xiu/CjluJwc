package com.yu.oa.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.core.domain.BaseEntity;

/**
 * 通知公告部门范围对象 oa_notice_dept
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public class OaNoticeDept extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 范围ID */
    private Long noticeDeptId;

    /** 公告ID */
    private Long noticeId;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    public Long getNoticeDeptId() { return noticeDeptId; }
    public void setNoticeDeptId(Long noticeDeptId) { this.noticeDeptId = noticeDeptId; }

    public Long getNoticeId() { return noticeId; }
    public void setNoticeId(Long noticeId) { this.noticeId = noticeId; }

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeDeptId", getNoticeDeptId())
            .append("noticeId", getNoticeId())
            .append("deptId", getDeptId())
            .append("deptName", getDeptName())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
