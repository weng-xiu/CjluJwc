package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmCourseLibrary;
import org.apache.ibatis.annotations.Param;

/**
 * 课程库Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmCourseLibraryMapper 
{
    public TpmCourseLibrary selectTpmCourseLibraryByCourseId(Long courseId);
    public TpmCourseLibrary selectTpmCourseLibraryByCourseCode(String courseCode);
    public List<TpmCourseLibrary> selectTpmCourseLibraryList(TpmCourseLibrary tpmCourseLibrary);
    public int insertTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary);
    public int updateTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary);
    public int deleteTpmCourseLibraryByCourseId(Long courseId);
    public int deleteTpmCourseLibraryByCourseIds(Long[] courseIds);

    /** 查询培养方案下的课程（可选按建议修读学期序号过滤），仅启用且未删除 */
    public List<TpmCourseLibrary> selectPlanCourses(@Param("planId") Long planId, @Param("semesterOrder") Integer semesterOrder);
}
