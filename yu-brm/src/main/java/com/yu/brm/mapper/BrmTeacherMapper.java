package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.domain.BrmTeacherQualification;

public interface BrmTeacherMapper
{
    public BrmTeacher selectBrmTeacherByTeacherId(Long teacherId);
    public BrmTeacher selectBrmTeacherByTeacherCode(String teacherCode);
    public BrmTeacher selectBrmTeacherByUserId(Long userId);
    public List<BrmTeacher> selectBrmTeacherList(BrmTeacher brmTeacher);
    public int insertBrmTeacher(BrmTeacher brmTeacher);
    public int updateBrmTeacher(BrmTeacher brmTeacher);
    public int updateBrmTeacherByUserId(BrmTeacher brmTeacher);
    public int deleteBrmTeacherByTeacherId(Long teacherId);
    public int deleteBrmTeacherByTeacherIds(Long[] teacherIds);
    public int deleteBrmTeacherByUserId(Long userId);
    public int deleteBrmTeacherPositionByTeacherIds(Long[] teacherIds);
    public int deleteBrmTeacherQualificationByTeacherIds(Long[] teacherIds);
    public int batchBrmTeacherPosition(List<BrmTeacherPosition> positionList);
    public int batchBrmTeacherQualification(List<BrmTeacherQualification> qualificationList);
    public int deleteBrmTeacherPositionByTeacherId(Long teacherId);
    public int deleteBrmTeacherQualificationByTeacherId(Long teacherId);
}
