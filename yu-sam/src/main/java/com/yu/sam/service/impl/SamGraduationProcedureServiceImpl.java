package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamGraduationProcedureMapper;
import com.yu.sam.domain.SamGraduationProcedure;
import com.yu.sam.service.ISamGraduationProcedureService;

@Service
public class SamGraduationProcedureServiceImpl implements ISamGraduationProcedureService 
{
    @Autowired
    private SamGraduationProcedureMapper samGraduationProcedureMapper;

    @Override
    public SamGraduationProcedure selectSamGraduationProcedureByProcedureId(Long procedureId) { return samGraduationProcedureMapper.selectSamGraduationProcedureByProcedureId(procedureId); }
    @Override
    public List<SamGraduationProcedure> selectSamGraduationProcedureList(SamGraduationProcedure samGraduationProcedure) { return samGraduationProcedureMapper.selectSamGraduationProcedureList(samGraduationProcedure); }
    @Override
    @Transactional
    public int insertSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure) { samGraduationProcedure.setCreateTime(DateUtils.getNowDate()); return samGraduationProcedureMapper.insertSamGraduationProcedure(samGraduationProcedure); }
    @Override
    @Transactional
    public int updateSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure) { samGraduationProcedure.setUpdateTime(DateUtils.getNowDate()); return samGraduationProcedureMapper.updateSamGraduationProcedure(samGraduationProcedure); }
    @Override
    @Transactional
    public int deleteSamGraduationProcedureByProcedureId(Long procedureId) { return samGraduationProcedureMapper.deleteSamGraduationProcedureByProcedureId(procedureId); }
    @Override
    @Transactional
    public int deleteSamGraduationProcedureByProcedureIds(Long[] procedureIds) { return samGraduationProcedureMapper.deleteSamGraduationProcedureByProcedureIds(procedureIds); }
}
