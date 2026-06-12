package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemExamSeatMapper;
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.service.IAemExamSeatService;

/**
 * 考场座位编排Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamSeatServiceImpl implements IAemExamSeatService 
{
    @Autowired
    private AemExamSeatMapper aemExamSeatMapper;

    @Override
    public AemExamSeat selectAemExamSeatBySeatId(Long seatId)
    {
        return aemExamSeatMapper.selectAemExamSeatBySeatId(seatId);
    }

    @Override
    public List<AemExamSeat> selectAemExamSeatList(AemExamSeat aemExamSeat)
    {
        return aemExamSeatMapper.selectAemExamSeatList(aemExamSeat);
    }

    @Override
    @Transactional
    public int insertAemExamSeat(AemExamSeat aemExamSeat)
    {
        // 数据范围校验：座位号必须为正整数
        if (aemExamSeat.getSeatNumber() != null && aemExamSeat.getSeatNumber() <= 0)
        {
            throw new ServiceException("座位号必须为正整数");
        }
        aemExamSeat.setCreateTime(DateUtils.getNowDate());
        return aemExamSeatMapper.insertAemExamSeat(aemExamSeat);
    }

    @Override
    @Transactional
    public int updateAemExamSeat(AemExamSeat aemExamSeat)
    {
        aemExamSeat.setUpdateTime(DateUtils.getNowDate());
        return aemExamSeatMapper.updateAemExamSeat(aemExamSeat);
    }

    @Override
    @Transactional
    public int deleteAemExamSeatBySeatId(Long seatId)
    {
        return aemExamSeatMapper.deleteAemExamSeatBySeatId(seatId);
    }

    @Override
    @Transactional
    public int deleteAemExamSeatBySeatIds(Long[] seatIds)
    {
        return aemExamSeatMapper.deleteAemExamSeatBySeatIds(seatIds);
    }
}
