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
    public List<SamStudent> selectSamStudentList(SamStudent samStudent);
    public int insertSamStudent(SamStudent samStudent);
    public int updateSamStudent(SamStudent samStudent);
    public int deleteSamStudentByStudentId(Long studentId);
    public int deleteSamStudentByStudentIds(Long[] studentIds);
}
