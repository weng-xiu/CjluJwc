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

    /**
     * 检查学期下是否存在考试计划
     */
    public int checkSemesterHasExamPlan(Long semesterId);

    /**
     * 检查学期下是否存在开课计划
     */
    public int checkSemesterHasCourseOffering(Long semesterId);
}
