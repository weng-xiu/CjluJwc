package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmSemester;

/**
 * 学期Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmSemesterService 
{
    public BrmSemester selectBrmSemesterBySemesterId(Long semesterId);
    public List<BrmSemester> selectBrmSemesterList(BrmSemester brmSemester);
    public int insertBrmSemester(BrmSemester brmSemester);
    public int updateBrmSemester(BrmSemester brmSemester);
    public int deleteBrmSemesterBySemesterId(Long semesterId);
    public int deleteBrmSemesterBySemesterIds(Long[] semesterIds);
}
