package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public int insertBrmSemester(BrmSemester brmSemester)
    {
        brmSemester.setCreateTime(DateUtils.getNowDate());
        return brmSemesterMapper.insertBrmSemester(brmSemester);
    }

    @Override
    public int updateBrmSemester(BrmSemester brmSemester)
    {
        brmSemester.setUpdateTime(DateUtils.getNowDate());
        return brmSemesterMapper.updateBrmSemester(brmSemester);
    }

    @Override
    public int deleteBrmSemesterBySemesterId(Long semesterId)
    {
        return brmSemesterMapper.deleteBrmSemesterBySemesterId(semesterId);
    }

    @Override
    public int deleteBrmSemesterBySemesterIds(Long[] semesterIds)
    {
        return brmSemesterMapper.deleteBrmSemesterBySemesterIds(semesterIds);
    }
}
