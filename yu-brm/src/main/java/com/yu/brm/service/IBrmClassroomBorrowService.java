package com.yu.brm.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

    /**
     * B1 借用冲突校验：排课占用（与周课表联动） + 其他在用借用单时段重叠
     *
     * @return 冲突描述列表，空表示无冲突
     */
    public List<String> checkConflict(Long classroomId, Date borrowDate, String startTime, String endTime, Long excludeBorrowId);

    /**
     * B1 教室占用日历：区间内借用单占用 + 该教室每周固定排课占用
     */
    public Map<String, Object> classroomOccupancy(Long classroomId, Date beginDate, Date endDate);
}
