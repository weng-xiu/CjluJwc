package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.domain.BrmTeacherQualification;

/**
 * 教师Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmTeacherService 
{
    public BrmTeacher selectBrmTeacherByTeacherId(Long teacherId);
    public List<BrmTeacher> selectBrmTeacherList(BrmTeacher brmTeacher);
    public int insertBrmTeacher(BrmTeacher brmTeacher);
    public int updateBrmTeacher(BrmTeacher brmTeacher);
    public int deleteBrmTeacherByTeacherId(Long teacherId);
    public int deleteBrmTeacherByTeacherIds(Long[] teacherIds);
}
