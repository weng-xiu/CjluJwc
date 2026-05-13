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
    public List<SamStudent> selectSamStudentList(SamStudent samStudent);
    public int insertSamStudent(SamStudent samStudent);
    public int updateSamStudent(SamStudent samStudent);
    public int deleteSamStudentByStudentIds(Long[] studentIds);
    public int deleteSamStudentByStudentId(Long studentId);
}
