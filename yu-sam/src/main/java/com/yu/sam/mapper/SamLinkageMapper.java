package com.yu.sam.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 学籍异动联动Mapper（跨模块表操作：选课冻结/成绩标记/缴费标记）
 * 与项目已有的SamWarningDataMapper直接查询aem_grade_record的模式一致。
 */
public interface SamLinkageMapper
{
    /**
     * 冻结学生选课记录（将 status 置为 '1' 停用）
     */
    public int freezeEnrollment(@Param("studentId") Long studentId);

    /**
     * 解冻学生选课记录（将 status 恢复为 '0' 正常）
     */
    public int unfreezeEnrollment(@Param("studentId") Long studentId);

    /**
     * 标记学生成绩记录（异动标记，设置 remark）
     */
    public int flagGradeRecords(@Param("studentId") Long studentId, @Param("flag") String flag);

    /**
     * 标记学生缴费状态（通过 sys_user 扩展字段或独立表，此处简化为 sam_student.status）
     */
    public int stopPayment(@Param("studentId") Long studentId);

    /**
     * 恢复缴费
     */
    public int resumePayment(@Param("studentId") Long studentId);
}
