package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmCourseLibrary;

/**
 * 课程库Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmCourseLibraryService 
{
    public TpmCourseLibrary selectTpmCourseLibraryByCourseId(Long courseId);
    public List<TpmCourseLibrary> selectTpmCourseLibraryList(TpmCourseLibrary tpmCourseLibrary);
    public int insertTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary);
    public int updateTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary);
    public int deleteTpmCourseLibraryByCourseIds(Long[] courseIds);
    public int deleteTpmCourseLibraryByCourseId(Long courseId);
}
