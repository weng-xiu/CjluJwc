package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.schedule.TimeSlotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.service.IScheduleOptimizationService;
import com.yu.tpm.service.ITpmScheduleService;

/**
 * 排课Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmScheduleServiceImpl implements ITpmScheduleService
{
    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Autowired
    private IScheduleOptimizationService scheduleOptimizationService;

    @Override
    public TpmSchedule selectTpmScheduleByScheduleId(Long scheduleId)
    {
        return tpmScheduleMapper.selectTpmScheduleByScheduleId(scheduleId);
    }

    @Override
    public List<TpmSchedule> selectTpmScheduleList(TpmSchedule tpmSchedule)
    {
        return tpmScheduleMapper.selectTpmScheduleList(tpmSchedule);
    }

    /**
     * 保存前校验：教室冲突 + 教师冲突
     *
     * @param schedule   当前排课
     * @param excludeId  编辑时排除自身的排课ID，新增时传 null
     */
    private void checkConflict(TpmSchedule schedule, Long excludeId)
    {
        boolean timeComplete = schedule.getClassroomId() != null
                && schedule.getWeekDay() != null && schedule.getStartPeriod() != null
                && schedule.getEndPeriod() != null && schedule.getStartWeek() != null
                && schedule.getEndWeek() != null;

        // 1. 教室冲突校验
        if (timeComplete)
        {
            boolean canAssign = scheduleOptimizationService.canAssignClassroom(
                    schedule.getClassroomId(), schedule.getWeekDay(), schedule.getStartPeriod(),
                    schedule.getEndPeriod(), schedule.getStartWeek(), schedule.getEndWeek());
            if (!canAssign)
            {
                // canAssignClassroom 不接受排除id，需手动过滤自身
                List<TpmSchedule> classroomUsed = tpmScheduleMapper.selectByClassroomAndTime(
                        schedule.getClassroomId(), schedule.getWeekDay(),
                        schedule.getStartPeriod(), schedule.getEndPeriod());
                boolean realConflict = false;
                if (classroomUsed != null)
                {
                    for (TpmSchedule s : classroomUsed)
                    {
                        if (excludeId != null && excludeId.equals(s.getScheduleId()))
                        {
                            continue;
                        }
                        if (TimeSlotUtils.hasWeeksOverlap(schedule.getStartWeek(), schedule.getEndWeek(),
                                s.getStartWeek(), s.getEndWeek()))
                        {
                            realConflict = true;
                            break;
                        }
                    }
                }
                if (realConflict)
                {
                    throw new ServiceException("所选教室在该时间段已有其他排课，存在教室冲突");
                }
            }
        }

        // 2. 教师冲突校验
        if (schedule.getWeekDay() != null && schedule.getStartPeriod() != null
                && schedule.getEndPeriod() != null && schedule.getStartWeek() != null
                && schedule.getEndWeek() != null && schedule.getOfferingId() != null)
        {
            TpmCourseOffering offering = tpmCourseOfferingMapper
                    .selectTpmCourseOfferingByOfferingId(schedule.getOfferingId());
            if (offering != null && offering.getTeacherId() != null)
            {
                List<TpmSchedule> teacherUsed = tpmScheduleMapper.selectByTeacherAndTime(
                        offering.getTeacherId(), schedule.getWeekDay(),
                        schedule.getStartPeriod(), schedule.getEndPeriod());
                if (teacherUsed != null)
                {
                    for (TpmSchedule s : teacherUsed)
                    {
                        if (excludeId != null && excludeId.equals(s.getScheduleId()))
                        {
                            continue;
                        }
                        if (TimeSlotUtils.hasWeeksOverlap(schedule.getStartWeek(), schedule.getEndWeek(),
                                s.getStartWeek(), s.getEndWeek()))
                        {
                            throw new ServiceException("该教师在该时间段已有其他排课，存在教师冲突");
                        }
                    }
                }
            }
        }
    }

    @Transactional
    @Override
    public int insertTpmSchedule(TpmSchedule tpmSchedule)
    {
        checkConflict(tpmSchedule, null);
        tpmSchedule.setCreateTime(DateUtils.getNowDate());
        return tpmScheduleMapper.insertTpmSchedule(tpmSchedule);
    }

    @Transactional
    @Override
    public int updateTpmSchedule(TpmSchedule tpmSchedule)
    {
        checkConflict(tpmSchedule, tpmSchedule.getScheduleId());
        tpmSchedule.setUpdateTime(DateUtils.getNowDate());
        return tpmScheduleMapper.updateTpmSchedule(tpmSchedule);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleByScheduleId(Long scheduleId)
    {
        return tpmScheduleMapper.deleteTpmScheduleByScheduleId(scheduleId);
    }

    @Transactional
    @Override
    public int deleteTpmScheduleByScheduleIds(Long[] scheduleIds)
    {
        return tpmScheduleMapper.deleteTpmScheduleByScheduleIds(scheduleIds);
    }
}
