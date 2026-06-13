package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamCertificate;
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

    @Autowired
    private SamStatusChangeMapper samStatusChangeMapper;

    @Autowired
    private SamCertificateMapper samCertificateMapper;

    @Override
    public SamStudent selectSamStudentByStudentId(Long studentId)
    {
        return samStudentMapper.selectSamStudentByStudentId(studentId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "s")
    public List<SamStudent> selectSamStudentList(SamStudent samStudent)
    {
        return samStudentMapper.selectSamStudentList(samStudent);
    }

    @Override
    @Transactional
    public int insertSamStudent(SamStudent samStudent)
    {
        // 唯一性校验：学号不能重复
        SamStudent query = new SamStudent();
        query.setStudentNo(samStudent.getStudentNo());
        List<SamStudent> existing = samStudentMapper.selectSamStudentList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("学号'" + samStudent.getStudentNo() + "'已存在");
        }
        samStudent.setCreateTime(DateUtils.getNowDate());
        return samStudentMapper.insertSamStudent(samStudent);
    }

    @Override
    @Transactional
    public int updateSamStudent(SamStudent samStudent)
    {
        // 状态校验：已毕业的学生不允许修改学籍信息
        SamStudent existing = samStudentMapper.selectSamStudentByStudentId(samStudent.getStudentId());
        if (existing != null && "3".equals(existing.getStudentStatus()))
        {
            throw new ServiceException("已毕业的学生不允许修改学籍信息");
        }
        samStudent.setUpdateTime(DateUtils.getNowDate());
        return samStudentMapper.updateSamStudent(samStudent);
    }

    @Override
    @Transactional
    public int deleteSamStudentByStudentId(Long studentId)
    {
        if (samStudentMapper.checkStudentHasGradeRecord(studentId) > 0)
        {
            throw new ServiceException("该学生存在成绩记录，不允许删除");
        }
        SamStatusChange scQuery = new SamStatusChange();
        scQuery.setStudentId(studentId);
        List<SamStatusChange> statusChanges = samStatusChangeMapper.selectSamStatusChangeList(scQuery);
        if (statusChanges != null && !statusChanges.isEmpty())
        {
            throw new ServiceException("该学生存在学籍异动记录，不允许删除");
        }
        SamCertificate certQuery = new SamCertificate();
        certQuery.setStudentId(studentId);
        List<SamCertificate> certificates = samCertificateMapper.selectSamCertificateList(certQuery);
        if (certificates != null && !certificates.isEmpty())
        {
            throw new ServiceException("该学生存在证书记录，不允许删除");
        }
        return samStudentMapper.deleteSamStudentByStudentId(studentId);
    }

    @Override
    @Transactional
    public int deleteSamStudentByStudentIds(Long[] studentIds)
    {
        for (Long studentId : studentIds)
        {
            if (samStudentMapper.checkStudentHasGradeRecord(studentId) > 0)
            {
                throw new ServiceException("该学生存在成绩记录，不允许删除");
            }
            SamStatusChange scQuery = new SamStatusChange();
            scQuery.setStudentId(studentId);
            List<SamStatusChange> statusChanges = samStatusChangeMapper.selectSamStatusChangeList(scQuery);
            if (statusChanges != null && !statusChanges.isEmpty())
            {
                throw new ServiceException("该学生存在学籍异动记录，不允许删除");
            }
            SamCertificate certQuery = new SamCertificate();
            certQuery.setStudentId(studentId);
            List<SamCertificate> certificates = samCertificateMapper.selectSamCertificateList(certQuery);
            if (certificates != null && !certificates.isEmpty())
            {
                throw new ServiceException("该学生存在证书记录，不允许删除");
            }
        }
        return samStudentMapper.deleteSamStudentByStudentIds(studentIds);
    }
}
