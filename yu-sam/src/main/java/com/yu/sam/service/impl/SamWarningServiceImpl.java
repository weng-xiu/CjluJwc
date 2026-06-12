package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamWarningMapper;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.service.ISamWarningService;

@Service
public class SamWarningServiceImpl implements ISamWarningService 
{
    @Autowired
    private SamWarningMapper samWarningMapper;

    @Override
    public SamWarning selectSamWarningByWarningId(Long warningId) { return samWarningMapper.selectSamWarningByWarningId(warningId); }
    @Override
    public List<SamWarning> selectSamWarningList(SamWarning samWarning) { return samWarningMapper.selectSamWarningList(samWarning); }
    @Override
    @Transactional
    public int insertSamWarning(SamWarning samWarning) { samWarning.setCreateTime(DateUtils.getNowDate()); return samWarningMapper.insertSamWarning(samWarning); }
    @Override
    @Transactional
    public int updateSamWarning(SamWarning samWarning) { samWarning.setUpdateTime(DateUtils.getNowDate()); return samWarningMapper.updateSamWarning(samWarning); }
    @Override
    @Transactional
    public int deleteSamWarningByWarningId(Long warningId) { return samWarningMapper.deleteSamWarningByWarningId(warningId); }
    @Override
    @Transactional
    public int deleteSamWarningByWarningIds(Long[] warningIds) { return samWarningMapper.deleteSamWarningByWarningIds(warningIds); }
}
