package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public int insertBrmAcademicYear(BrmAcademicYear brmAcademicYear)
    {
        // 唯一性校验：学年名称不能重复
        BrmAcademicYear query = new BrmAcademicYear();
        query.setYearName(brmAcademicYear.getYearName());
        List<BrmAcademicYear> existing = brmAcademicYearMapper.selectBrmAcademicYearList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("学年名称'" + brmAcademicYear.getYearName() + "'已存在");
        }
        brmAcademicYear.setCreateTime(DateUtils.getNowDate());
        return brmAcademicYearMapper.insertBrmAcademicYear(brmAcademicYear);
    }

    @Override
    @Transactional
    public int updateBrmAcademicYear(BrmAcademicYear brmAcademicYear)
    {
        // 唯一性校验：学年名称不能重复（排除自身）
        BrmAcademicYear query = new BrmAcademicYear();
        query.setYearName(brmAcademicYear.getYearName());
        List<BrmAcademicYear> existing = brmAcademicYearMapper.selectBrmAcademicYearList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (BrmAcademicYear item : existing)
            {
                if (!item.getYearId().equals(brmAcademicYear.getYearId()))
                {
                    throw new ServiceException("学年名称'" + brmAcademicYear.getYearName() + "'已存在");
                }
            }
        }
        brmAcademicYear.setUpdateTime(DateUtils.getNowDate());
        return brmAcademicYearMapper.updateBrmAcademicYear(brmAcademicYear);
    }

    @Override
    @Transactional
    public int deleteBrmAcademicYearByYearId(Long yearId)
    {
        return brmAcademicYearMapper.deleteBrmAcademicYearByYearId(yearId);
    }

    @Override
    @Transactional
    public int deleteBrmAcademicYearByYearIds(Long[] yearIds)
    {
        return brmAcademicYearMapper.deleteBrmAcademicYearByYearIds(yearIds);
    }
}
