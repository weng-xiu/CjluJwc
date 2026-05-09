package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmAcademicYear;

/**
 * 学年Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmAcademicYearService 
{
    public BrmAcademicYear selectBrmAcademicYearByYearId(Long yearId);
    public List<BrmAcademicYear> selectBrmAcademicYearList(BrmAcademicYear brmAcademicYear);
    public int insertBrmAcademicYear(BrmAcademicYear brmAcademicYear);
    public int updateBrmAcademicYear(BrmAcademicYear brmAcademicYear);
    public int deleteBrmAcademicYearByYearId(Long yearId);
    public int deleteBrmAcademicYearByYearIds(Long[] yearIds);
}
