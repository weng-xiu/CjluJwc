package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmCourseOffering;
import org.apache.ibatis.annotations.Param;

/**
 * 开课计划Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmCourseOfferingMapper 
{
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId);
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering);

    /** 门户端：可选课程列表（附带学分/已选人数，无数据范围过滤） */
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering);
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId);
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds);

    /** 查询指定学期已存在（未删除且未取消）开课的课程ID集合，用于批量生成幂等去重 */
    public List<Long> selectActiveCourseIdsBySemester(@Param("semesterId") Long semesterId);

    /** 查询可用教师池（在职 status=0），可选按院系过滤 */
    public List<Long> selectTeacherPool(@Param("deptId") Long deptId);

    /** 批量插入开课计划 */
    public int batchInsertTpmCourseOffering(@Param("list") List<TpmCourseOffering> list);
}
