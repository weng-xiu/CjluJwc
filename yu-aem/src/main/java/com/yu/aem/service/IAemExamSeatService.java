package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemExamSeat;

/**
 * 考场座位编排Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemExamSeatService 
{
    public AemExamSeat selectAemExamSeatBySeatId(Long seatId);
    public List<AemExamSeat> selectAemExamSeatList(AemExamSeat aemExamSeat);
    public int insertAemExamSeat(AemExamSeat aemExamSeat);
    public int updateAemExamSeat(AemExamSeat aemExamSeat);
    public int deleteAemExamSeatBySeatIds(Long[] seatIds);
    public int deleteAemExamSeatBySeatId(Long seatId);

    /**
     * 自动编排考场座位
     * 按学号排序，蛇形分配座位，同考场考生错开相邻座位
     *
     * @param examId      考试ID
     * @param classroomId 教室ID
     * @return 编排结果统计
     */
    public java.util.Map<String, Object> autoArrangeSeats(Long examId, Long classroomId);

    /**
     * 批量导入座位安排
     *
     * @param list      座位记录列表
     * @param operator  操作人
     * @return 导入条数
     */
    public int importSeat(List<AemExamSeat> list, String operator);
}
