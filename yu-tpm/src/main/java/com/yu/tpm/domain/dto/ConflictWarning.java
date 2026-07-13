package com.yu.tpm.domain.dto;

import java.io.Serializable;

/**
 * 选课冲突警告信息
 *
 * @author ruoyi
 */
public class ConflictWarning implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 冲突类型：TIME_CONFLICT时间冲突 / CREDIT_OVERFLOW学分超限 / COURSE_FULL课程已满 / PREREQUISITE_MISSING前置课程缺失 */
    private String conflictType;

    /** 冲突描述 */
    private String message;

    /** 冲突的已选课程ID */
    private Long conflictCourseId;

    /** 冲突课程名称 */
    private String conflictCourseName;

    /** 冲突时间描述 */
    private String conflictTimeDesc;

    public ConflictWarning()
    {
    }

    public ConflictWarning(String conflictType, String message)
    {
        this.conflictType = conflictType;
        this.message = message;
    }

    public ConflictWarning(String conflictType, String message, Long conflictCourseId, String conflictCourseName, String conflictTimeDesc)
    {
        this.conflictType = conflictType;
        this.message = message;
        this.conflictCourseId = conflictCourseId;
        this.conflictCourseName = conflictCourseName;
        this.conflictTimeDesc = conflictTimeDesc;
    }

    public String getConflictType() { return conflictType; }
    public void setConflictType(String conflictType) { this.conflictType = conflictType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getConflictCourseId() { return conflictCourseId; }
    public void setConflictCourseId(Long conflictCourseId) { this.conflictCourseId = conflictCourseId; }

    public String getConflictCourseName() { return conflictCourseName; }
    public void setConflictCourseName(String conflictCourseName) { this.conflictCourseName = conflictCourseName; }

    public String getConflictTimeDesc() { return conflictTimeDesc; }
    public void setConflictTimeDesc(String conflictTimeDesc) { this.conflictTimeDesc = conflictTimeDesc; }
}
