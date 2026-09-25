package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamStudent;

/**
 * 学生学籍Service接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface ISamStudentService 
{
    public SamStudent selectSamStudentByStudentId(Long studentId);

    /** P6：门户端按登录用户ID查本人学籍（含院系/专业/班级名称） */
    public SamStudent selectSamStudentByUserId(Long userId);

    public List<SamStudent> selectSamStudentList(SamStudent samStudent);
    public int insertSamStudent(SamStudent samStudent);
    public int updateSamStudent(SamStudent samStudent);
    public int deleteSamStudentByStudentIds(Long[] studentIds);
    public int deleteSamStudentByStudentId(Long studentId);

    /** P7：学籍批量导入（逐行校验并生成校验报告） */
    public String importStudent(List<SamStudent> studentList, String operName, boolean updateSupport);
}
