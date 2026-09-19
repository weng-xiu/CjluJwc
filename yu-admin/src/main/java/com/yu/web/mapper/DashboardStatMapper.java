package com.yu.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 教务数据驾驶舱统计Mapper（P2，只读跨模块聚合）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
public interface DashboardStatMapper
{
    /** 当前学期（覆盖今天的学期，无则取最新启用学期） */
    public Map<String, Object> selectCurrentSemester();

    /** 核心计数指标卡 */
    public Map<String, Object> selectCoreCounts(@Param("semesterId") Long semesterId);

    /** 未解除预警按级别分布 */
    public List<Map<String, Object>> selectWarningLevelDistribution();

    /** 学期课程通过率最低 TOP N（挂科率视角） */
    public List<Map<String, Object>> selectCoursePassTop(@Param("semesterId") Long semesterId, @Param("limit") int limit);

    /** 学期选课结果分布（0待抽签 1已选 2落选 3退选，按实际字典） */
    public List<Map<String, Object>> selectEnrollResultDistribution(@Param("semesterId") Long semesterId);
}
