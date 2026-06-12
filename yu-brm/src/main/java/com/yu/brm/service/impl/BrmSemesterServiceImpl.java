package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmSemesterMapper;
import com.yu.brm.domain.BrmSemester;
import com.yu.brm.service.IBrmSemesterService;

/**
 * 学期Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmSemesterServiceImpl implements IBrmSemesterService 
{
    @Autowired
    private BrmSemesterMapper brmSemesterMapper;

    @Override
    public BrmSemester selectBrmSemesterBySemesterId(Long semesterId)
    {
        return brmSemesterMapper.selectBrmSemesterBySemesterId(semesterId);
    }

    @Override
    public List<BrmSemester> selectBrmSemesterList(BrmSemester brmSemester)
    {
        return brmSemesterMapper.selectBrmSemesterList(brmSemester);
    }

    @Override
    @Transactional
    public int insertBrmSemester(BrmSemester brmSemester)
    {
        // 唯一性校验：同学年下学期名称不能重复
        BrmSemester query = new BrmSemester();
        query.setSemesterName(brmSemester.getSemesterName());
        query.setAcademicYearId(brmSemester.getAcademicYearId());
        List<BrmSemester> existing = brmSemesterMapper.selectBrmSemesterList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("该学年下学期名称'" + brmSemester.getSemesterName() + "'已存在");
        }
        brmSemester.setCreateTime(DateUtils.getNowDate());
        return brmSemesterMapper.insertBrmSemester(brmSemester);
    }

    @Override
    @Transactional
    public int updateBrmSemester(BrmSemester brmSemester)
    {
        // 唯一性校验：同学年下学期名称不能重复（排除自身）
        BrmSemester query = new BrmSemester();
        query.setSemesterName(brmSemester.getSemesterName());
        query.setAcademicYearId(brmSemester.getAcademicYearId());
        List<BrmSemester> existing = brmSemesterMapper.selectBrmSemesterList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (BrmSemester item : existing)
            {
                if (!item.getSemesterId().equals(brmSemester.getSemesterId()))
                {
                    throw new ServiceException("该学年下学期名称'" + brmSemester.getSemesterName() + "'已存在");
                }
            }
        }
        brmSemester.setUpdateTime(DateUtils.getNowDate());
        return brmSemesterMapper.updateBrmSemester(brmSemester);
    }

    @Override
    @Transactional
    public int deleteBrmSemesterBySemesterId(Long semesterId)
    {
        if (brmSemesterMapper.checkSemesterHasExamPlan(semesterId) > 0)
        {
            throw new ServiceException("该学期下存在考试计划，不允许删除");
        }
        if (brmSemesterMapper.checkSemesterHasCourseOffering(semesterId) > 0)
        {
            throw new ServiceException("该学期下存在开课计划，不允许删除");
        }
        return brmSemesterMapper.deleteBrmSemesterBySemesterId(semesterId);
    }

    @Override
    @Transactional
    public int deleteBrmSemesterBySemesterIds(Long[] semesterIds)
    {
        for (Long semesterId : semesterIds)
        {
            if (brmSemesterMapper.checkSemesterHasExamPlan(semesterId) > 0)
            {
                throw new ServiceException("该学期下存在考试计划，不允许删除");
            }
            if (brmSemesterMapper.checkSemesterHasCourseOffering(semesterId) > 0)
            {
                throw new ServiceException("该学期下存在开课计划，不允许删除");
            }
        }
        return brmSemesterMapper.deleteBrmSemesterBySemesterIds(semesterIds);
    }
}
