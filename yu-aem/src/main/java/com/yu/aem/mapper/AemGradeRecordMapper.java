package com.yu.aem.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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
    public List<AemGradeRecord> selectByStudentAndSemester(@Param("studentId") Long studentId, @Param("semesterId") Long semesterId);
    public List<AemGradeRecord> selectBySemester(@Param("semesterId") Long semesterId);
    public int updateGradePointBatch(List<AemGradeRecord> records);

    /** 批量新增成绩记录（Excel导入使用） */
    public int batchInsert(List<AemGradeRecord> list);

    public Double selectCourseCreditByCourseId(Long courseId);
}
