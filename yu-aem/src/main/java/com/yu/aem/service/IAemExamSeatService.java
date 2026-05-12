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
}
