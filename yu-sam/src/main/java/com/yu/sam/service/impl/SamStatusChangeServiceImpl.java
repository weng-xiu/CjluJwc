package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.service.ISamStatusChangeService;

@Service
public class SamStatusChangeServiceImpl implements ISamStatusChangeService 
{
    @Autowired
    private SamStatusChangeMapper samStatusChangeMapper;

    @Override
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId) { return samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId); }
    @Override
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange) { return samStatusChangeMapper.selectSamStatusChangeList(samStatusChange); }
    @Override
    public int insertSamStatusChange(SamStatusChange samStatusChange) { samStatusChange.setCreateTime(DateUtils.getNowDate()); return samStatusChangeMapper.insertSamStatusChange(samStatusChange); }
    @Override
    public int updateSamStatusChange(SamStatusChange samStatusChange) { samStatusChange.setUpdateTime(DateUtils.getNowDate()); return samStatusChangeMapper.updateSamStatusChange(samStatusChange); }
    @Override
    public int deleteSamStatusChangeByChangeId(Long changeId) { return samStatusChangeMapper.deleteSamStatusChangeByChangeId(changeId); }
    @Override
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds) { return samStatusChangeMapper.deleteSamStatusChangeByChangeIds(changeIds); }
}
