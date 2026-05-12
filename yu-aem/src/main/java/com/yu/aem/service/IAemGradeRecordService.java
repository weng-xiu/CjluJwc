package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeRecord;

/**
 * 成绩记录Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemGradeRecordService 
{
    public AemGradeRecord selectAemGradeRecordByGradeId(Long gradeId);
    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord);
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds);
    public int deleteAemGradeRecordByGradeId(Long gradeId);
}
