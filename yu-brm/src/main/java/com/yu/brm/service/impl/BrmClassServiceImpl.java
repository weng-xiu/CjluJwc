package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmClassMapper;
import com.yu.brm.domain.BrmClass;
import com.yu.brm.service.IBrmClassService;

/**
 * 班级Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmClassServiceImpl implements IBrmClassService 
{
    @Autowired
    private BrmClassMapper brmClassMapper;

    @Override
    public BrmClass selectBrmClassByClassId(Long classId)
    {
        return brmClassMapper.selectBrmClassByClassId(classId);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<BrmClass> selectBrmClassList(BrmClass brmClass)
    {
        return brmClassMapper.selectBrmClassList(brmClass);
    }

    @Override
    @Transactional
    public int insertBrmClass(BrmClass brmClass)
    {
        brmClass.setCreateTime(DateUtils.getNowDate());
        return brmClassMapper.insertBrmClass(brmClass);
    }

    @Override
    @Transactional
    public int updateBrmClass(BrmClass brmClass)
    {
        brmClass.setUpdateTime(DateUtils.getNowDate());
        return brmClassMapper.updateBrmClass(brmClass);
    }

    @Override
    @Transactional
    public int deleteBrmClassByClassId(Long classId)
    {
        return brmClassMapper.deleteBrmClassByClassId(classId);
    }

    @Override
    @Transactional
    public int deleteBrmClassByClassIds(Long[] classIds)
    {
        return brmClassMapper.deleteBrmClassByClassIds(classIds);
    }
}
