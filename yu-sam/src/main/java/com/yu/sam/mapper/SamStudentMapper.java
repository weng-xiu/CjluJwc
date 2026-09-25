package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamStudent;

/**
 * 学生学籍Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamStudentMapper 
{
    public SamStudent selectSamStudentByStudentId(Long studentId);

    /** P6：门户端按登录用户ID查本人学籍（含院系/专业/班级名称，不走数据权限过滤） */
    public SamStudent selectSamStudentByUserId(Long userId);

    public List<SamStudent> selectSamStudentList(SamStudent samStudent);
    public int insertSamStudent(SamStudent samStudent);
    public int updateSamStudent(SamStudent samStudent);
    public int deleteSamStudentByStudentId(Long studentId);
    public int deleteSamStudentByStudentIds(Long[] studentIds);

    /**
     * 检查学生是否存在成绩记录
     */
    public int checkStudentHasGradeRecord(Long studentId);

    /** P7 导入：按学号精确查询（业务唯一键） */
    public SamStudent selectSamStudentByStudentNo(String studentNo);

    /** P7 导入：专业ID存在性校验 */
    public int countMajorExists(Long majorId);

    /** P7 导入：院系ID存在性校验 */
    public int countDeptExists(Long deptId);

    /** P7 导入：班级ID存在性校验 */
    public int countClassExists(Long classId);

    /** P7 导入：身份证号冲突校验（排除自身） */
    public int countIdCardConflict(@org.apache.ibatis.annotations.Param("idCard") String idCard, @org.apache.ibatis.annotations.Param("studentId") Long studentId);
}
