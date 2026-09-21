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

    /** 门户端：学生个人成绩列表（附带课程名/学分/学期名，无数据范围过滤） */
    public List<AemGradeRecord> selectAemGradeRecordListForPortal(AemGradeRecord aemGradeRecord);
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

    /** A5：教师提交成绩（未提交/已驳回 → 已提交待审），锁定记录不受影响 */
    public int submitGradeBatch(@Param("gradeIds") Long[] gradeIds,
                                @Param("submitBy") String submitBy,
                                @Param("submitTime") java.util.Date submitTime);

    /** A5：教研室审核（通过→锁定2 / 驳回→3），仅作用于已提交待审记录 */
    public int auditGradeBatch(@Param("gradeIds") Long[] gradeIds,
                               @Param("targetStatus") String targetStatus,
                               @Param("auditBy") String auditBy,
                               @Param("auditTime") java.util.Date auditTime);

    /** A5：管理解锁（锁定2 → 已驳回3），异常纠正用 */
    public int unlockGradeBatch(@Param("gradeIds") Long[] gradeIds,
                                @Param("auditBy") String auditBy,
                                @Param("auditTime") java.util.Date auditTime);
}
