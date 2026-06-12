package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmMajorMapper;
import com.yu.brm.mapper.BrmClassMapper;
import com.yu.brm.domain.BrmMajor;
import com.yu.brm.domain.BrmClass;
import com.yu.brm.service.IBrmMajorService;

/**
 * 专业Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmMajorServiceImpl implements IBrmMajorService 
{
    @Autowired
    private BrmMajorMapper brmMajorMapper;

    @Autowired
    private BrmClassMapper brmClassMapper;

    @Override
    public BrmMajor selectBrmMajorByMajorId(Long majorId)
    {
        return brmMajorMapper.selectBrmMajorByMajorId(majorId);
    }

    @Override
    public List<BrmMajor> selectBrmMajorList(BrmMajor brmMajor)
    {
        return brmMajorMapper.selectBrmMajorList(brmMajor);
    }

    @Override
    @Transactional
    public int insertBrmMajor(BrmMajor brmMajor)
    {
        brmMajor.setCreateTime(DateUtils.getNowDate());
        return brmMajorMapper.insertBrmMajor(brmMajor);
    }

    @Override
    @Transactional
    public int updateBrmMajor(BrmMajor brmMajor)
    {
        brmMajor.setUpdateTime(DateUtils.getNowDate());
        return brmMajorMapper.updateBrmMajor(brmMajor);
    }

    @Override
    @Transactional
    public int deleteBrmMajorByMajorId(Long majorId)
    {
        BrmClass query = new BrmClass();
        query.setMajorId(majorId);
        List<BrmClass> classes = brmClassMapper.selectBrmClassList(query);
        if (classes != null && !classes.isEmpty())
        {
            throw new ServiceException("该专业下存在班级，不允许删除");
        }
        return brmMajorMapper.deleteBrmMajorByMajorId(majorId);
    }

    @Override
    @Transactional
    public int deleteBrmMajorByMajorIds(Long[] majorIds)
    {
        for (Long majorId : majorIds)
        {
            BrmClass query = new BrmClass();
            query.setMajorId(majorId);
            List<BrmClass> classes = brmClassMapper.selectBrmClassList(query);
            if (classes != null && !classes.isEmpty())
            {
                throw new ServiceException("该专业下存在班级，不允许删除");
            }
        }
        return brmMajorMapper.deleteBrmMajorByMajorIds(majorIds);
    }
}
