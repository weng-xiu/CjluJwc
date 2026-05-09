package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmClassroomBorrow;

/**
 * 教室借用Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmClassroomBorrowService 
{
    public BrmClassroomBorrow selectBrmClassroomBorrowByBorrowId(Long borrowId);
    public List<BrmClassroomBorrow> selectBrmClassroomBorrowList(BrmClassroomBorrow brmClassroomBorrow);
    public int insertBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow);
    public int updateBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow);
    public int deleteBrmClassroomBorrowByBorrowId(Long borrowId);
    public int deleteBrmClassroomBorrowByBorrowIds(Long[] borrowIds);
}
