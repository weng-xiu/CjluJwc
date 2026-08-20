package com.yu.tpm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.tpm.domain.TpmSelectionEnrollment;

/**
 * 选课名单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmSelectionEnrollmentMapper 
{
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds);

    /**
     * 查询某开课的已选人数
     *
     * @param courseOfferingId 开课ID
     * @return 已选人数
     */
    public int selectCountByOffering(@Param("courseOfferingId") Long courseOfferingId);

    /**
     * 查询学生在某轮次已选的选课记录
     *
     * @param studentId 学生ID
     * @param roundId   轮次ID
     * @return 选课记录列表
     */
    public List<TpmSelectionEnrollment> selectByStudentAndRound(@Param("studentId") Long studentId, @Param("roundId") Long roundId);

    /**
     * 统计学生已完成（选中）的指定课程数量（先修课校验用）
     *
     * @param studentId 学生ID
     * @param courseIds 课程ID列表
     * @return 已完成的课程门数
     */
    public int countCompletedCourses(@Param("studentId") Long studentId, @Param("courseIds") List<Long> courseIds);
}
