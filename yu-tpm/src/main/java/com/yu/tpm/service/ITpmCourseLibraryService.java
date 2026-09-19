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

    /**
     * P7：批量导入课程（含逐行校验），返回校验报告
     *
     * @param courseList 待导入课程列表
     * @param operName   操作人
     * @param updateSupport 编码已存在时是否更新
     * @return 导入结果报告
     */
    public String importCourse(List<TpmCourseLibrary> courseList, String operName, boolean updateSupport);
}
