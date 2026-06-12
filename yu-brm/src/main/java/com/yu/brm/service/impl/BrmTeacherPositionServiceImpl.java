package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmTeacherPositionMapper;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.service.IBrmTeacherPositionService;

/**
 * 教师任职信息Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmTeacherPositionServiceImpl implements IBrmTeacherPositionService 
{
    @Autowired
    private BrmTeacherPositionMapper brmTeacherPositionMapper;

    @Override
    public BrmTeacherPosition selectBrmTeacherPositionByPosId(Long posId)
    {
        return brmTeacherPositionMapper.selectBrmTeacherPositionByPosId(posId);
    }

    @Override
    public List<BrmTeacherPosition> selectBrmTeacherPositionList(BrmTeacherPosition brmTeacherPosition)
    {
        return brmTeacherPositionMapper.selectBrmTeacherPositionList(brmTeacherPosition);
    }

    @Override
    @Transactional
    public int insertBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition)
    {
        brmTeacherPosition.setCreateTime(DateUtils.getNowDate());
        return brmTeacherPositionMapper.insertBrmTeacherPosition(brmTeacherPosition);
    }

    @Override
    @Transactional
    public int updateBrmTeacherPosition(BrmTeacherPosition brmTeacherPosition)
    {
        brmTeacherPosition.setUpdateTime(DateUtils.getNowDate());
        return brmTeacherPositionMapper.updateBrmTeacherPosition(brmTeacherPosition);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherPositionByPosId(Long posId)
    {
        return brmTeacherPositionMapper.deleteBrmTeacherPositionByPosId(posId);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherPositionByPosIds(Long[] posIds)
    {
        return brmTeacherPositionMapper.deleteBrmTeacherPositionByPosIds(posIds);
    }
}
