package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemSupervisionRecord;

/**
 * 督导听课记录Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemSupervisionRecordService 
{
    public AemSupervisionRecord selectAemSupervisionRecordByRecordId(Long recordId);
    public List<AemSupervisionRecord> selectAemSupervisionRecordList(AemSupervisionRecord aemSupervisionRecord);
    public int insertAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord);
    public int updateAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord);
    public int deleteAemSupervisionRecordByRecordIds(Long[] recordIds);
    public int deleteAemSupervisionRecordByRecordId(Long recordId);
}
