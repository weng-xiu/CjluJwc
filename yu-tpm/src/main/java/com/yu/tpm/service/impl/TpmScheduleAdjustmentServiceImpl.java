package com.yu.tpm.service.impl;

import java.util.Date;
import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.schedule.TimeSlotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmScheduleAdjustment;
import com.yu.tpm.mapper.TpmScheduleAdjustmentMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.service.ITpmScheduleAdjustmentService;

/**
 * 调停课申请Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmScheduleAdjustmentServiceImpl implements ITpmScheduleAdjustmentService 
{
    @Autowired
    private TpmScheduleAdjustmentMapper tpmScheduleAdjustmentMapper;

    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Override
    public TpmScheduleAdjustment selectTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Override
    public List<TpmScheduleAdjustment> selectTpmScheduleAdjustmentList(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        return tpmScheduleAdjustmentMapper.selectTpmScheduleAdjustmentList(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int insertTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setCreateTime(DateUtils.getNowDate());
        // 新增时默认待审
        if (tpmScheduleAdjustment.getApproveStatus() == null
                || tpmScheduleAdjustment.getApproveStatus().trim().isEmpty())
        {
            tpmScheduleAdjustment.setApproveStatus("0");
        }
        return tpmScheduleAdjustmentMapper.insertTpmScheduleAdjustment(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int updateTpmScheduleAdjustment(TpmScheduleAdjustment tpmScheduleAdjustment)
    {
        tpmScheduleAdjustment.setUpdateTime(DateUtils.getNowDate());
        return tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(tpmScheduleAdjustment);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustId(Long adjustId)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustId(adjustId);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleAdjustmentByAdjustIds(Long[] adjustIds)
    {
        return tpmScheduleAdjustmentMapper.deleteTpmScheduleAdjustmentByAdjustIds(adjustIds);
    }

    @Transactional
    @Override
    public void approve(Long adjustId, String approveComment)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentMapper
                .selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!"0".equals(adjustment.getApproveStatus()))
        {
            throw new ServiceException("该申请已审批，不能重复审批");
        }

        // 联动回写排课表
        boolean needUpdateSchedule = adjustment.getNewClassroomId() != null
                || adjustment.getNewWeekDay() != null
                || adjustment.getNewStartPeriod() != null
                || adjustment.getNewEndPeriod() != null;
        if (needUpdateSchedule)
        {
            TpmSchedule original = tpmScheduleMapper.selectTpmScheduleByScheduleId(adjustment.getScheduleId());
            if (original == null)
            {
                throw new ServiceException("原排课记录不存在，无法回写");
            }

            Long targetClassroomId = adjustment.getNewClassroomId() != null
                    ? adjustment.getNewClassroomId() : original.getClassroomId();
            Integer targetWeekDay = adjustment.getNewWeekDay() != null
                    ? adjustment.getNewWeekDay() : original.getWeekDay();
            Integer targetStartPeriod = adjustment.getNewStartPeriod() != null
                    ? adjustment.getNewStartPeriod() : original.getStartPeriod();
            Integer targetEndPeriod = adjustment.getNewEndPeriod() != null
                    ? adjustment.getNewEndPeriod() : original.getEndPeriod();

            // 新教室冲突检测
            if (adjustment.getNewClassroomId() != null)
            {
                List<TpmSchedule> conflicts = tpmScheduleMapper.selectByClassroomAndTime(
                        targetClassroomId, targetWeekDay, targetStartPeriod, targetEndPeriod);
                if (conflicts != null)
                {
                    int startWeek = original.getStartWeek() == null ? 1 : original.getStartWeek();
                    int endWeek = original.getEndWeek() == null ? 20 : original.getEndWeek();
                    for (TpmSchedule other : conflicts)
                    {
                        if (other.getScheduleId().equals(original.getScheduleId()))
                        {
                            continue;
                        }
                        int otherStartWeek = other.getStartWeek() == null ? 1 : other.getStartWeek();
                        int otherEndWeek = other.getEndWeek() == null ? 20 : other.getEndWeek();
                        if (TimeSlotUtils.hasWeeksOverlap(startWeek, endWeek, otherStartWeek, otherEndWeek))
                        {
                            throw new ServiceException("审批失败：新教室在该时间段与其他排课冲突");
                        }
                    }
                }
            }

            TpmSchedule update = new TpmSchedule();
            update.setScheduleId(original.getScheduleId());
            if (adjustment.getNewClassroomId() != null)
            {
                update.setClassroomId(adjustment.getNewClassroomId());
            }
            if (adjustment.getNewWeekDay() != null)
            {
                update.setWeekDay(adjustment.getNewWeekDay());
            }
            if (adjustment.getNewStartPeriod() != null)
            {
                update.setStartPeriod(adjustment.getNewStartPeriod());
            }
            if (adjustment.getNewEndPeriod() != null)
            {
                update.setEndPeriod(adjustment.getNewEndPeriod());
            }
            tpmScheduleMapper.updateTpmSchedule(update);
        }

        TpmScheduleAdjustment update = new TpmScheduleAdjustment();
        update.setAdjustId(adjustId);
        update.setApproveStatus("1");
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        update.setApproveComment(approveComment);
        tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(update);
    }

    @Transactional
    @Override
    public void reject(Long adjustId, String approveComment)
    {
        TpmScheduleAdjustment adjustment = tpmScheduleAdjustmentMapper
                .selectTpmScheduleAdjustmentByAdjustId(adjustId);
        if (adjustment == null)
        {
            throw new ServiceException("调停课申请不存在");
        }
        if (!"0".equals(adjustment.getApproveStatus()))
        {
            throw new ServiceException("该申请已审批，不能重复审批");
        }

        TpmScheduleAdjustment update = new TpmScheduleAdjustment();
        update.setAdjustId(adjustId);
        update.setApproveStatus("2");
        update.setApproveBy(SecurityUtils.getUsername());
        update.setApproveTime(new Date());
        update.setApproveComment(approveComment);
        tpmScheduleAdjustmentMapper.updateTpmScheduleAdjustment(update);
    }
}
