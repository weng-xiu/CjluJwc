package com.yu.brm.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 教室借用冲突校验与占用日历Mapper（B1）
 * 只读跨表查询：直查 tpm_schedule/tpm_course_offering（与 B2 统计同范式，避免依赖 yu-tpm 造成循环）
 *
 * @author yu
 * @date 2026-09-21
 */
public interface BrmBorrowConflictMapper
{
    /** 借用日期与排课星期冲突数（该教室当日星期是否存在有效排课） */
    public int countScheduleConflict(@Param("classroomId") Long classroomId, @Param("borrowDate") Date borrowDate);

    /** 冲突排课明细（课程/星期/节次/教师），用于提示与占用日历 */
    public List<Map<String, Object>> listScheduleConflict(@Param("classroomId") Long classroomId, @Param("borrowDate") Date borrowDate);

    /** 与其他在用借用单（待审/审批中/已通过）的时段重叠数 */
    public int countBorrowConflict(@Param("classroomId") Long classroomId, @Param("borrowDate") Date borrowDate,
                                   @Param("startTime") String startTime, @Param("endTime") String endTime,
                                   @Param("excludeBorrowId") Long excludeBorrowId);

    /** 教室占用日历：日期区间内在用借用单 */
    public List<Map<String, Object>> listBorrowOccupancy(@Param("classroomId") Long classroomId,
                                                         @Param("beginDate") Date beginDate, @Param("endDate") Date endDate);

    /** 教室每周固定排课占用（全星期），用于占用日历底纹 */
    public List<Map<String, Object>> listWeeklySchedule(@Param("classroomId") Long classroomId);
}
