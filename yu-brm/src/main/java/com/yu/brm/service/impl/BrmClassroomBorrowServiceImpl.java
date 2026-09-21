package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmClassroomBorrowMapper;
import com.yu.brm.mapper.BrmBorrowConflictMapper;
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

    @Autowired
    private BrmBorrowConflictMapper brmBorrowConflictMapper;

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
        // B1：新增借用默认为待院系审核
        if (brmClassroomBorrow.getApproveStatus() == null
                || brmClassroomBorrow.getApproveStatus().trim().isEmpty())
        {
            brmClassroomBorrow.setApproveStatus("0");
        }
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

    @Override
    public List<String> checkConflict(Long classroomId, Date borrowDate, String startTime, String endTime, Long excludeBorrowId)
    {
        List<String> conflicts = new ArrayList<>();
        if (classroomId == null || borrowDate == null)
        {
            return conflicts;
        }
        // 1) 排课占用冲突：借用日期对应星期上该教室有有效排课
        int scheduleConflict = brmBorrowConflictMapper.countScheduleConflict(classroomId, borrowDate);
        if (scheduleConflict > 0)
        {
            List<Map<String, Object>> details = brmBorrowConflictMapper.listScheduleConflict(classroomId, borrowDate);
            StringBuilder sb = new StringBuilder("该教室当日与排课冲突：");
            int n = 0;
            for (Map<String, Object> d : details)
            {
                if (n++ >= 3)
                {
                    sb.append(" 等");
                    break;
                }
                sb.append("【").append(d.get("courseName")).append(" ")
                  .append("周").append(weekDayText(d.get("weekDay"))).append(" ")
                  .append(d.get("startPeriod")).append("-").append(d.get("endPeriod")).append("节】");
            }
            conflicts.add(sb.toString());
        }
        // 2) 其他在用借用单时段重叠冲突
        int borrowConflict = brmBorrowConflictMapper.countBorrowConflict(classroomId, borrowDate, startTime, endTime, excludeBorrowId);
        if (borrowConflict > 0)
        {
            conflicts.add("该教室在所选时段已有 " + borrowConflict + " 条待审/审批中/已通过的借用申请");
        }
        return conflicts;
    }

    @Override
    public Map<String, Object> classroomOccupancy(Long classroomId, Date beginDate, Date endDate)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("borrows", classroomId != null
                ? brmBorrowConflictMapper.listBorrowOccupancy(classroomId, beginDate, endDate)
                : new ArrayList<>());
        result.put("weeklySchedule", classroomId != null
                ? brmBorrowConflictMapper.listWeeklySchedule(classroomId)
                : new ArrayList<>());
        return result;
    }

    /** 星期数字转中文（1=周一..7=周日） */
    private String weekDayText(Object weekDay)
    {
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        try
        {
            int w = Integer.parseInt(String.valueOf(weekDay));
            if (w >= 1 && w <= 7)
            {
                return names[w - 1];
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        return String.valueOf(weekDay);
    }
}
