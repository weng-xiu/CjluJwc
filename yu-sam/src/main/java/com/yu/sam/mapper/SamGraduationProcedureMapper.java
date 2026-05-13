package com.yu.sam.mapper;

import java.util.List;
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
}
