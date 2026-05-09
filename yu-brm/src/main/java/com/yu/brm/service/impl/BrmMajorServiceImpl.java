package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmMajorMapper;
import com.yu.brm.domain.BrmMajor;
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
    public int insertBrmMajor(BrmMajor brmMajor)
    {
        brmMajor.setCreateTime(DateUtils.getNowDate());
        return brmMajorMapper.insertBrmMajor(brmMajor);
    }

    @Override
    public int updateBrmMajor(BrmMajor brmMajor)
    {
        brmMajor.setUpdateTime(DateUtils.getNowDate());
        return brmMajorMapper.updateBrmMajor(brmMajor);
    }

    @Override
    public int deleteBrmMajorByMajorId(Long majorId)
    {
        return brmMajorMapper.deleteBrmMajorByMajorId(majorId);
    }

    @Override
    public int deleteBrmMajorByMajorIds(Long[] majorIds)
    {
        return brmMajorMapper.deleteBrmMajorByMajorIds(majorIds);
    }
}
