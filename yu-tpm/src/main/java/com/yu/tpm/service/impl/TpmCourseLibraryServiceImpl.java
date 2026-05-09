package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.domain.TpmCourseLibrary;
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

    @Override
    public int insertTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        tpmCourseLibrary.setCreateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.insertTpmCourseLibrary(tpmCourseLibrary);
    }

    @Override
    public int updateTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        tpmCourseLibrary.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.updateTpmCourseLibrary(tpmCourseLibrary);
    }

    @Override
    public int deleteTpmCourseLibraryByCourseId(Long courseId)
    {
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseId(courseId);
    }

    @Override
    public int deleteTpmCourseLibraryByCourseIds(Long[] courseIds)
    {
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseIds(courseIds);
    }
}
