package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.domain.BrmTeacherQualification;
import com.yu.brm.service.IBrmTeacherService;

/**
 * 教师Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmTeacherServiceImpl implements IBrmTeacherService 
{
    @Autowired
    private BrmTeacherMapper brmTeacherMapper;

    @Override
    public BrmTeacher selectBrmTeacherByTeacherId(Long teacherId)
    {
        return brmTeacherMapper.selectBrmTeacherByTeacherId(teacherId);
    }

    @Override
    public List<BrmTeacher> selectBrmTeacherList(BrmTeacher brmTeacher)
    {
        return brmTeacherMapper.selectBrmTeacherList(brmTeacher);
    }

    @Override
    @Transactional
    public int insertBrmTeacher(BrmTeacher brmTeacher)
    {
        brmTeacher.setCreateTime(DateUtils.getNowDate());
        int rows = brmTeacherMapper.insertBrmTeacher(brmTeacher);
        insertBrmTeacherPosition(brmTeacher);
        insertBrmTeacherQualification(brmTeacher);
        return rows;
    }

    @Override
    @Transactional
    public int updateBrmTeacher(BrmTeacher brmTeacher)
    {
        brmTeacher.setUpdateTime(DateUtils.getNowDate());
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherId(brmTeacher.getTeacherId());
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherId(brmTeacher.getTeacherId());
        insertBrmTeacherPosition(brmTeacher);
        insertBrmTeacherQualification(brmTeacher);
        return brmTeacherMapper.updateBrmTeacher(brmTeacher);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherByTeacherId(Long teacherId)
    {
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherId(teacherId);
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherId(teacherId);
        return brmTeacherMapper.deleteBrmTeacherByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherByTeacherIds(Long[] teacherIds)
    {
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherIds(teacherIds);
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherIds(teacherIds);
        return brmTeacherMapper.deleteBrmTeacherByTeacherIds(teacherIds);
    }

    public void insertBrmTeacherPosition(BrmTeacher brmTeacher)
    {
        List<BrmTeacherPosition> positionList = brmTeacher.getPositionList();
        Long teacherId = brmTeacher.getTeacherId();
        if (StringUtils.isNotNull(positionList))
        {
            List<BrmTeacherPosition> list = new ArrayList<BrmTeacherPosition>();
            for (BrmTeacherPosition position : positionList)
            {
                position.setTeacherId(teacherId);
                list.add(position);
            }
            if (list.size() > 0)
            {
                brmTeacherMapper.batchBrmTeacherPosition(list);
            }
        }
    }

    public void insertBrmTeacherQualification(BrmTeacher brmTeacher)
    {
        List<BrmTeacherQualification> qualificationList = brmTeacher.getQualificationList();
        Long teacherId = brmTeacher.getTeacherId();
        if (StringUtils.isNotNull(qualificationList))
        {
            List<BrmTeacherQualification> list = new ArrayList<BrmTeacherQualification>();
            for (BrmTeacherQualification qualification : qualificationList)
            {
                qualification.setTeacherId(teacherId);
                list.add(qualification);
            }
            if (list.size() > 0)
            {
                brmTeacherMapper.batchBrmTeacherQualification(list);
            }
        }
    }
}
