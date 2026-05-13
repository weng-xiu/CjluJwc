package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamGraduationProcedure;

public interface ISamGraduationProcedureService 
{
    public SamGraduationProcedure selectSamGraduationProcedureByProcedureId(Long procedureId);
    public List<SamGraduationProcedure> selectSamGraduationProcedureList(SamGraduationProcedure samGraduationProcedure);
    public int insertSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int updateSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure);
    public int deleteSamGraduationProcedureByProcedureIds(Long[] procedureIds);
    public int deleteSamGraduationProcedureByProcedureId(Long procedureId);
}
