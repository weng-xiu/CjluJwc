package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmSemester;

public interface BrmSemesterMapper 
{
    public BrmSemester selectBrmSemesterBySemesterId(Long semesterId);
    public List<BrmSemester> selectBrmSemesterList(BrmSemester brmSemester);
    public int insertBrmSemester(BrmSemester brmSemester);
    public int updateBrmSemester(BrmSemester brmSemester);
    public int deleteBrmSemesterBySemesterId(Long semesterId);
    public int deleteBrmSemesterBySemesterIds(Long[] semesterIds);
}
