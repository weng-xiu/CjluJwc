package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmAcademicYearMapper;
import com.yu.brm.domain.BrmAcademicYear;
import com.yu.brm.service.IBrmAcademicYearService;

/**
 * 学年Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmAcademicYearServiceImpl implements IBrmAcademicYearService 
{
    @Autowired
    private BrmAcademicYearMapper brmAcademicYearMapper;

    @Override
    public BrmAcademicYear selectBrmAcademicYearByYearId(Long yearId)
    {
        return brmAcademicYearMapper.selectBrmAcademicYearByYearId(yearId);
    }

    @Override
    public List<BrmAcademicYear> selectBrmAcademicYearList(BrmAcademicYear brmAcademicYear)
    {
        return brmAcademicYearMapper.selectBrmAcademicYearList(brmAcademicYear);
    }

    @Override
    public int insertBrmAcademicYear(BrmAcademicYear brmAcademicYear)
    {
        brmAcademicYear.setCreateTime(DateUtils.getNowDate());
        return brmAcademicYearMapper.insertBrmAcademicYear(brmAcademicYear);
    }

    @Override
    public int updateBrmAcademicYear(BrmAcademicYear brmAcademicYear)
    {
        brmAcademicYear.setUpdateTime(DateUtils.getNowDate());
        return brmAcademicYearMapper.updateBrmAcademicYear(brmAcademicYear);
    }

    @Override
    public int deleteBrmAcademicYearByYearId(Long yearId)
    {
        return brmAcademicYearMapper.deleteBrmAcademicYearByYearId(yearId);
    }

    @Override
    public int deleteBrmAcademicYearByYearIds(Long[] yearIds)
    {
        return brmAcademicYearMapper.deleteBrmAcademicYearByYearIds(yearIds);
    }
}
