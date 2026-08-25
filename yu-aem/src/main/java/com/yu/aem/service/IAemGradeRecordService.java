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

    /** 查询成绩记录明细（含复核记录子表） */
    public AemGradeRecord selectAemGradeRecordDetail(Long gradeId);

    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord);
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds);
    public int deleteAemGradeRecordByGradeId(Long gradeId);

    /** 计算学生某学期GPA */
    public Double calculateStudentGpa(Long studentId, Long semesterId, String algorithmCode);

    /** 批量重算某学期所有学生GPA */
    public void batchRecalculateGpa(Long semesterId, String algorithmCode);
}
