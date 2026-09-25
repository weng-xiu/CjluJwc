package com.yu.sam.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamProcedureItem;

/**
 * 学生离校环节办理明细Mapper接口（S7）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public interface SamProcedureItemMapper
{
    public List<SamProcedureItem> selectItemsByProcedureId(@Param("procedureId") Long procedureId);

    public SamProcedureItem selectItemByProcedureAndStep(@Param("procedureId") Long procedureId, @Param("stepId") Long stepId);

    public SamProcedureItem selectSamProcedureItemByItemId(Long itemId);

    public int insertSamProcedureItem(SamProcedureItem samProcedureItem);

    public int updateSamProcedureItem(SamProcedureItem samProcedureItem);

    /** 按 (procedure_id, step_id) upsert 置为已办（checkType：0人工 1自动） */
    public int upsertItemDone(@Param("procedureId") Long procedureId, @Param("studentId") Long studentId,
                             @Param("stepId") Long stepId, @Param("checkType") String checkType,
                             @Param("checkBy") String checkBy, @Param("checkTime") Date checkTime);

    /** 为学生已有手续记录按启用环节补齐办理明细（NOT EXISTS 幂等），返回新增条数 */
    public int initItemsForProcedures(@Param("studentId") Long studentId);

    /** 自动判定数据源：返回指定条件下已办结手续对应的学生ID（source=LEGACY/CARD/CERT_PICKUP/GRAD_REVIEW/DEGREE_REVIEW） */
    public List<Long> selectAutoCheckStudentIds(@Param("source") String source, @Param("column") String column);

    /** 按学生ID批量查询其手续ID（student_id/procedure_id） */
    public List<Map<String, Object>> selectProcedureIdsByStudents(@Param("studentIds") List<Long> studentIds);

    /** O1：按学生ID取关联的系统用户ID（离校办结通知） */
    public Long selectStudentUserId(@Param("studentId") Long studentId);
}
