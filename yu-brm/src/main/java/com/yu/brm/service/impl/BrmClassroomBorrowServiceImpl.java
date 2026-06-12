package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmClassroomBorrowMapper;
import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;

/**
 * 教室借用Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmClassroomBorrowServiceImpl implements IBrmClassroomBorrowService 
{
    @Autowired
    private BrmClassroomBorrowMapper brmClassroomBorrowMapper;

    @Override
    public BrmClassroomBorrow selectBrmClassroomBorrowByBorrowId(Long borrowId)
    {
        return brmClassroomBorrowMapper.selectBrmClassroomBorrowByBorrowId(borrowId);
    }

    @Override
    public List<BrmClassroomBorrow> selectBrmClassroomBorrowList(BrmClassroomBorrow brmClassroomBorrow)
    {
        return brmClassroomBorrowMapper.selectBrmClassroomBorrowList(brmClassroomBorrow);
    }

    @Override
    @Transactional
    public int insertBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow)
    {
        brmClassroomBorrow.setCreateTime(DateUtils.getNowDate());
        return brmClassroomBorrowMapper.insertBrmClassroomBorrow(brmClassroomBorrow);
    }

    @Override
    @Transactional
    public int updateBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow)
    {
        brmClassroomBorrow.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomBorrowMapper.updateBrmClassroomBorrow(brmClassroomBorrow);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomBorrowByBorrowId(Long borrowId)
    {
        return brmClassroomBorrowMapper.deleteBrmClassroomBorrowByBorrowId(borrowId);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomBorrowByBorrowIds(Long[] borrowIds)
    {
        return brmClassroomBorrowMapper.deleteBrmClassroomBorrowByBorrowIds(borrowIds);
    }
}
