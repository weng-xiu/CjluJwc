package com.yu.brm.service;

import java.util.List;
import java.util.Map;

/**
 * 资源利用分析Service接口（B2）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface IBrmResourceStatService
{
    /** 资源总览：教室/设备/维保/教师核心指标 */
    public Map<String, Object> overview(Long semesterId);

    /** 教室利用率明细（附 utilizationRate 百分比与 TOP 排名） */
    public List<Map<String, Object>> classroomUtilization(Long semesterId, Long buildingId);

    /** 教师工作量明细 */
    public List<Map<String, Object>> teacherWorkload(Long semesterId);

    /** 维保到期提醒列表 */
    public List<Map<String, Object>> maintenanceDue();
}
