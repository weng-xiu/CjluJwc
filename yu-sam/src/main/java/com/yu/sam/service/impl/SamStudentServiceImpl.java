package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.service.ISamStudentService;

/**
 * 学生学籍Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
@Service
public class SamStudentServiceImpl implements ISamStudentService 
{
    @Autowired
    private SamStudentMapper samStudentMapper;

    @Override
    public SamStudent selectSamStudentByStudentId(Long studentId)
    {
        return samStudentMapper.selectSamStudentByStudentId(studentId);
    }

    @Override
    public List<SamStudent> selectSamStudentList(SamStudent samStudent)
    {
        return samStudentMapper.selectSamStudentList(samStudent);
    }

    @Override
    public int insertSamStudent(SamStudent samStudent)
    {
        samStudent.setCreateTime(DateUtils.getNowDate());
        return samStudentMapper.insertSamStudent(samStudent);
    }

    @Override
    public int updateSamStudent(SamStudent samStudent)
    {
        samStudent.setUpdateTime(DateUtils.getNowDate());
        return samStudentMapper.updateSamStudent(samStudent);
    }

    @Override
    public int deleteSamStudentByStudentId(Long studentId)
    {
        return samStudentMapper.deleteSamStudentByStudentId(studentId);
    }

    @Override
    public int deleteSamStudentByStudentIds(Long[] studentIds)
    {
        return samStudentMapper.deleteSamStudentByStudentIds(studentIds);
    }
}
