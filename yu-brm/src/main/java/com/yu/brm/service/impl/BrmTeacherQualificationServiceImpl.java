package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmTeacherQualificationMapper;
import com.yu.brm.domain.BrmTeacherQualification;
import com.yu.brm.service.IBrmTeacherQualificationService;

/**
 * 教师授课资格Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmTeacherQualificationServiceImpl implements IBrmTeacherQualificationService 
{
    @Autowired
    private BrmTeacherQualificationMapper brmTeacherQualificationMapper;

    @Override
    public BrmTeacherQualification selectBrmTeacherQualificationByQualId(Long qualId)
    {
        return brmTeacherQualificationMapper.selectBrmTeacherQualificationByQualId(qualId);
    }

    @Override
    public List<BrmTeacherQualification> selectBrmTeacherQualificationList(BrmTeacherQualification brmTeacherQualification)
    {
        return brmTeacherQualificationMapper.selectBrmTeacherQualificationList(brmTeacherQualification);
    }

    @Override
    @Transactional
    public int insertBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification)
    {
        brmTeacherQualification.setCreateTime(DateUtils.getNowDate());
        return brmTeacherQualificationMapper.insertBrmTeacherQualification(brmTeacherQualification);
    }

    @Override
    @Transactional
    public int updateBrmTeacherQualification(BrmTeacherQualification brmTeacherQualification)
    {
        brmTeacherQualification.setUpdateTime(DateUtils.getNowDate());
        return brmTeacherQualificationMapper.updateBrmTeacherQualification(brmTeacherQualification);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherQualificationByQualId(Long qualId)
    {
        return brmTeacherQualificationMapper.deleteBrmTeacherQualificationByQualId(qualId);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherQualificationByQualIds(Long[] qualIds)
    {
        return brmTeacherQualificationMapper.deleteBrmTeacherQualificationByQualIds(qualIds);
    }
}
