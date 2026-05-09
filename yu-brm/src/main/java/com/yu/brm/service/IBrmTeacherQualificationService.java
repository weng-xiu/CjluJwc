package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmTeacherQualification;

/**
 * 教师授课资格Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmTeacherQualificationService 
{
    public BrmTeacherQualification selectBrmTeacherQualificationByQualId(Long qualId);
    public List<BrmTeacherQualification> selectBrmTeacherQualificationList(BrmTeacherQualification brmTeacherQualification);
    public int insertBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification);
    public int updateBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification);
    public int deleteBrmTeacherQualificationByQualId(Long qualId);
    public int deleteBrmTeacherQualificationByQualIds(Long[] qualIds);
}
