package com.yu.aem.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.aem.domain.AemExamSeat;

/**
 * 考场座位编排Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemExamSeatMapper 
{
    public AemExamSeat selectAemExamSeatBySeatId(Long seatId);
    public List<AemExamSeat> selectAemExamSeatList(AemExamSeat aemExamSeat);
    public int insertAemExamSeat(AemExamSeat aemExamSeat);
    public int updateAemExamSeat(AemExamSeat aemExamSeat);
    public int deleteAemExamSeatBySeatId(Long seatId);
    public int deleteAemExamSeatBySeatIds(Long[] seatIds);

    /** 根据考试ID删除座位记录 */
    public int deleteByExamId(Long examId);

    /** 批量插入座位记录 */
    public int batchInsert(List<AemExamSeat> seats);

    /** 查询课程+学期的选课学生ID列表（跨表查询） */
    public List<Long> selectStudentIdsByCourseAndSemester(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /**
     * A1：查询指定日期与时段内已被占用的教室ID集合（来自已编排座位关联考试计划），
     * 用于自动编排时跳过冲突教室。excludeExamId 用于排除当前考试（重排场景）。
     */
    public List<Long> selectOccupiedClassroomIds(@Param("examDate") java.util.Date examDate,
                                                 @Param("startTime") String startTime,
                                                 @Param("endTime") String endTime,
                                                 @Param("excludeExamId") Long excludeExamId);

    /** A2：查询某考试已占用的教室ID集合（去重）。 */
    public List<Long> selectClassroomIdsByExamId(@Param("examId") Long examId);

    /** A2：查询某考试的考生学生ID集合（去重）。 */
    public List<Long> selectStudentIdsByExamId(@Param("examId") Long examId);
}
