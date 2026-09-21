package com.yu.brm.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 资源利用分析Mapper接口（B2：教室利用率、设备状态与维保提醒、教师工作量）
 *
 * 说明：只读聚合查询，跨库表 JOIN tpm_schedule/tpm_course_offering 计算真实利用率。
 * 全局关闭驼峰转换，列名统一 as camelCase 别名。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface BrmResourceStatMapper
{
    /** 教室维度：排课节次占用（scheduleCount/weeklySessions），按学期可选过滤 */
    public List<Map<String, Object>> classroomUtilization(@Param("semesterId") Long semesterId,
                                                          @Param("buildingId") Long buildingId);

    /** 设备状态汇总：按 status 分组计数 */
    public List<Map<String, Object>> equipmentStatusSummary();

    /** 维保到期提醒：正常设备中，最近维保距今超过 dueDays 或从未维保 */
    public List<Map<String, Object>> maintenanceDueList(@Param("dueDays") int dueDays);

    /** 教师工作量：按教师聚合课程数、开课数、排课节次 */
    public List<Map<String, Object>> teacherWorkload(@Param("semesterId") Long semesterId);
}
