package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmTeacherQualification;

public interface BrmTeacherQualificationMapper 
{
    public BrmTeacherQualification selectBrmTeacherQualificationByQualId(Long qualId);
    public List<BrmTeacherQualification> selectBrmTeacherQualificationList(BrmTeacherQualification brmTeacherQualification);
    public int insertBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification);
    public int updateBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification);
    public int deleteBrmTeacherQualificationByQualId(Long qualId);
    public int deleteBrmTeacherQualificationByQualIds(Long[] qualIds);
}
