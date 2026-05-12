package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemSupervisionRecord;

/**
 * 督导听课记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemSupervisionRecordMapper 
{
    public AemSupervisionRecord selectAemSupervisionRecordByRecordId(Long recordId);
    public List<AemSupervisionRecord> selectAemSupervisionRecordList(AemSupervisionRecord aemSupervisionRecord);
    public int insertAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord);
    public int updateAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord);
    public int deleteAemSupervisionRecordByRecordId(Long recordId);
    public int deleteAemSupervisionRecordByRecordIds(Long[] recordIds);
}

