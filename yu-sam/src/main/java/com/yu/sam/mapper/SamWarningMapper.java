package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamWarning;

/**
 * 学籍预警Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamWarningMapper 
{
    public SamWarning selectSamWarningByWarningId(Long warningId);
    public List<SamWarning> selectSamWarningList(SamWarning samWarning);
    public int insertSamWarning(SamWarning samWarning);
    public int updateSamWarning(SamWarning samWarning);
    public int deleteSamWarningByWarningId(Long warningId);
    public int deleteSamWarningByWarningIds(Long[] warningIds);
}
