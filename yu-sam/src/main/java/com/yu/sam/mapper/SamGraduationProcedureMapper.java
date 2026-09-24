package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamGraduationProcedure;

/**
 * 毕业离校手续Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamGraduationProcedureMapper 
{
    public SamGraduationProcedure selectSamGraduationProcedureByProcedureId(Long procedureId);
    public List<SamGraduationProcedure> selectSamGraduationProcedureList(SamGraduationProcedure samGraduationProcedure);
    public int insertSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int updateSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int deleteSamGraduationProcedureByProcedureId(Long procedureId);
    public int deleteSamGraduationProcedureByProcedureIds(Long[] procedureIds);

    /** 按学生ID查手续记录 */
    public SamGraduationProcedure selectByStudentId(@Param("studentId") Long studentId);

    /** 为已毕业学生批量初始化未建手续记录（NOT EXISTS 幂等） */
    public int initProceduresForGraduates(@Param("studentId") Long studentId, @Param("createBy") String createBy);

    /** 离校办理总览：total/completed/inProgress/notStarted */
    public java.util.Map<String, Object> statOverview();
}
