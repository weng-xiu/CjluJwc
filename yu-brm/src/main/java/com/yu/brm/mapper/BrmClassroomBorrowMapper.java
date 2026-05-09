package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmClassroomBorrow;

public interface BrmClassroomBorrowMapper 
{
    public BrmClassroomBorrow selectBrmClassroomBorrowByBorrowId(Long borrowId);
    public List<BrmClassroomBorrow> selectBrmClassroomBorrowList(BrmClassroomBorrow brmClassroomBorrow);
    public int insertBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow);
    public int updateBrmClassroomBorrow(BrmClassroomBorrow brmClassroomBorrow);
    public int deleteBrmClassroomBorrowByBorrowId(Long borrowId);
    public int deleteBrmClassroomBorrowByBorrowIds(Long[] borrowIds);
}
