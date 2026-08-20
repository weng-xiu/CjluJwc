package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseLibraryService;

/**
 * 课程库Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCourseLibraryServiceImpl implements ITpmCourseLibraryService 
{
    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Override
    public TpmCourseLibrary selectTpmCourseLibraryByCourseId(Long courseId)
    {
        return tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(courseId);
    }

    @Override
    public List<TpmCourseLibrary> selectTpmCourseLibraryList(TpmCourseLibrary tpmCourseLibrary)
    {
        return tpmCourseLibraryMapper.selectTpmCourseLibraryList(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int insertTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        // 唯一性校验：课程编码不能重复
        TpmCourseLibrary existing = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(tpmCourseLibrary.getCourseCode());
        if (existing != null)
        {
            throw new ServiceException("课程编码'" + tpmCourseLibrary.getCourseCode() + "'已存在");
        }
        tpmCourseLibrary.setCreateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.insertTpmCourseLibrary(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int updateTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        // 唯一性校验：课程编码不能重复（排除自身）
        TpmCourseLibrary existing = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(tpmCourseLibrary.getCourseCode());
        if (existing != null && !existing.getCourseId().equals(tpmCourseLibrary.getCourseId()))
        {
            throw new ServiceException("课程编码'" + tpmCourseLibrary.getCourseCode() + "'已存在");
        }
        tpmCourseLibrary.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.updateTpmCourseLibrary(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int deleteTpmCourseLibraryByCourseId(Long courseId)
    {
        TpmCourseOffering query = new TpmCourseOffering();
        query.setCourseId(courseId);
        List<TpmCourseOffering> offerings = tpmCourseOfferingMapper.selectTpmCourseOfferingList(query);
        if (offerings != null && !offerings.isEmpty())
        {
            throw new ServiceException("该课程下存在开课计划，不允许删除");
        }
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseId(courseId);
    }

    @Transactional
    @Override
    public int deleteTpmCourseLibraryByCourseIds(Long[] courseIds)
    {
        for (Long courseId : courseIds)
        {
            TpmCourseOffering query = new TpmCourseOffering();
            query.setCourseId(courseId);
            List<TpmCourseOffering> offerings = tpmCourseOfferingMapper.selectTpmCourseOfferingList(query);
            if (offerings != null && !offerings.isEmpty())
            {
                throw new ServiceException("该课程下存在开课计划，不允许删除");
            }
        }
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseIds(courseIds);
    }
}
