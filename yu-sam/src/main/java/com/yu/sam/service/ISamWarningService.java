package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamWarning;

public interface ISamWarningService 
{
    public SamWarning selectSamWarningByWarningId(Long warningId);
    public List<SamWarning> selectSamWarningList(SamWarning samWarning);
    public int insertSamWarning(SamWarning samWarning);
    public int updateSamWarning(SamWarning samWarning);
    public int deleteSamWarningByWarningIds(Long[] warningIds);
    public int deleteSamWarningByWarningId(Long warningId);
}
