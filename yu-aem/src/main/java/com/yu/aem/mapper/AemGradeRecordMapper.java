package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemGradeRecord;

/**
 * 成绩记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemGradeRecordMapper 
{
    public AemGradeRecord selectAemGradeRecordByGradeId(Long gradeId);
    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord);
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int deleteAemGradeRecordByGradeId(Long gradeId);
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds);
}
