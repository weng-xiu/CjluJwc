package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamStatusChange;

public interface ISamStatusChangeService 
{
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId);
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange);
    public int insertSamStatusChange(SamStatusChange samStatusChange);
    public int updateSamStatusChange(SamStatusChange samStatusChange);
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds);
    public int deleteSamStatusChangeByChangeId(Long changeId);
}
