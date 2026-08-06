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
}
