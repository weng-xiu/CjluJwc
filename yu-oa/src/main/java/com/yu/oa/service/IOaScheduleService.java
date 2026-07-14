package com.yu.oa.service;

import java.util.List;
import com.yu.oa.domain.OaSchedule;

/**
 * 日程安排Service接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOaScheduleService 
{
    /**
     * 查询日程安排
     * 
     * @param scheduleId 日程安排ID
     * @return 日程安排
     */
    public OaSchedule selectOaScheduleByScheduleId(Long scheduleId);

    /**
     * 查询日程安排列表
     * 
     * @param oaSchedule 日程安排
     * @return 日程安排集合
     */
    public List<OaSchedule> selectOaScheduleList(OaSchedule oaSchedule);

    /**
     * 新增日程安排
     * 
     * @param oaSchedule 日程安排
     * @return 结果
     */
    public int insertOaSchedule(OaSchedule oaSchedule);

    /**
     * 修改日程安排
     * 
     * @param oaSchedule 日程安排
     * @return 结果
     */
    public int updateOaSchedule(OaSchedule oaSchedule);

    /**
     * 批量删除日程安排
     * 
     * @param scheduleIds 需要删除的日程安排ID
     * @return 结果
     */
    public int deleteOaScheduleByScheduleIds(Long[] scheduleIds);

    /**
     * 删除日程安排信息
     * 
     * @param scheduleId 日程安排ID
     * @return 结果
     */
    public int deleteOaScheduleByScheduleId(Long scheduleId);
}
