package com.yu.oa.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaSchedule;
import com.yu.oa.domain.OaScheduleShare;
import com.yu.oa.mapper.OaScheduleMapper;
import com.yu.oa.mapper.OaScheduleShareMapper;
import com.yu.oa.service.IOaScheduleService;

/**
 * 日程安排Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaScheduleServiceImpl implements IOaScheduleService
{
    @Autowired
    private OaScheduleMapper oaScheduleMapper;

    @Autowired
    private OaScheduleShareMapper oaScheduleShareMapper;

    /**
     * 查询日程安排
     * 
     * @param scheduleId 日程安排ID
     * @return 日程安排
     */
    @Override
    public OaSchedule selectOaScheduleByScheduleId(Long scheduleId)
    {
        OaSchedule schedule = oaScheduleMapper.selectOaScheduleByScheduleId(scheduleId);
        if (schedule != null)
        {
            schedule.setShareList(oaScheduleShareMapper.selectOaScheduleShareByScheduleId(scheduleId));
        }
        return schedule;
    }

    /**
     * 查询日程安排列表
     * 
     * @param oaSchedule 日程安排
     * @return 日程安排集合
     */
    @Override
    public List<OaSchedule> selectOaScheduleList(OaSchedule oaSchedule)
    {
        return oaScheduleMapper.selectOaScheduleList(oaSchedule);
    }

    /**
     * 新增日程安排
     * 
     * @param oaSchedule 日程安排
     * @return 结果
     */
    @Override
    @Transactional
    public int insertOaSchedule(OaSchedule oaSchedule)
    {
        oaSchedule.setCreateTime(DateUtils.getNowDate());
        oaSchedule.setOwnerId(SecurityUtils.getLoginUser().getUserId());
        oaSchedule.setOwnerName(SecurityUtils.getUsername());
        int rows = oaScheduleMapper.insertOaSchedule(oaSchedule);
        insertOaScheduleShare(oaSchedule);
        return rows;
    }

    /**
     * 修改日程安排
     * 
     * @param oaSchedule 日程安排
     * @return 结果
     */
    @Override
    @Transactional
    public int updateOaSchedule(OaSchedule oaSchedule)
    {
        oaSchedule.setUpdateTime(DateUtils.getNowDate());
        oaScheduleShareMapper.deleteOaScheduleShareByScheduleId(oaSchedule.getScheduleId());
        insertOaScheduleShare(oaSchedule);
        return oaScheduleMapper.updateOaSchedule(oaSchedule);
    }

    /**
     * 批量删除日程安排
     * 
     * @param scheduleIds 需要删除的日程安排ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteOaScheduleByScheduleIds(Long[] scheduleIds)
    {
        for (Long scheduleId : scheduleIds)
        {
            oaScheduleShareMapper.deleteOaScheduleShareByScheduleId(scheduleId);
        }
        return oaScheduleMapper.deleteOaScheduleByScheduleIds(scheduleIds);
    }

    /**
     * 删除日程安排信息
     * 
     * @param scheduleId 日程安排ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteOaScheduleByScheduleId(Long scheduleId)
    {
        oaScheduleShareMapper.deleteOaScheduleShareByScheduleId(scheduleId);
        return oaScheduleMapper.deleteOaScheduleByScheduleId(scheduleId);
    }

    /**
     * 新增共享人员
     * 
     * @param oaSchedule 日程安排对象
     */
    private void insertOaScheduleShare(OaSchedule oaSchedule)
    {
        List<OaScheduleShare> shareList = oaSchedule.getShareList();
        if (shareList == null || shareList.isEmpty())
        {
            return;
        }
        for (OaScheduleShare share : shareList)
        {
            share.setScheduleId(oaSchedule.getScheduleId());
            share.setCreateTime(DateUtils.getNowDate());
            oaScheduleShareMapper.insertOaScheduleShare(share);
        }
    }
}
