package com.yu.web.service;

import java.util.Map;

/**
 * 教务数据驾驶舱Service接口（P2）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface IDashboardStatService
{
    /**
     * 驾驶舱总览：当前学期 + 核心指标 + 预警分布 + 低通过率课程TOP + 选课结果分布
     */
    public Map<String, Object> selectOverview();
}
