package com.yu.web.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.web.mapper.DashboardStatMapper;
import com.yu.web.service.IDashboardStatService;

/**
 * 教务数据驾驶舱Service实现（P2）
 * 全部指标来自真实业务表聚合，替代原若依模板 mock 看板。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Service
public class DashboardStatServiceImpl implements IDashboardStatService
{
    /** 预警级别名称（0一般 1严重 2高危） */
    private static final String[] WARNING_LEVEL_NAMES = { "一般", "严重", "高危" };

    /** 选课结果名称（1已选 2落选 3退选） */
    private static final String[] ENROLL_RESULT_NAMES = { "其他", "已选", "落选", "退选" };

    @Autowired
    private DashboardStatMapper dashboardStatMapper;

    @Override
    public Map<String, Object> selectOverview()
    {
        Map<String, Object> overview = new HashMap<>();

        // 1. 当前学期
        Map<String, Object> semester = dashboardStatMapper.selectCurrentSemester();
        Long semesterId = null;
        if (semester != null && semester.get("semesterId") != null)
        {
            semesterId = ((Number) semester.get("semesterId")).longValue();
        }
        overview.put("semester", semester);

        // 2. 核心指标卡
        overview.put("counts", dashboardStatMapper.selectCoreCounts(semesterId));

        // 3. 预警级别分布（饼图）
        List<Map<String, Object>> warningDist = new ArrayList<>();
        List<Map<String, Object>> warnings = dashboardStatMapper.selectWarningLevelDistribution();
        if (warnings != null)
        {
            for (Map<String, Object> row : warnings)
            {
                Map<String, Object> item = new HashMap<>();
                item.put("name", levelName(row.get("level")));
                item.put("value", row.get("total"));
                warningDist.add(item);
            }
        }
        overview.put("warningDistribution", warningDist);

        // 4. 低通过率课程 TOP10（条形图）
        overview.put("coursePassTop", dashboardStatMapper.selectCoursePassTop(semesterId, 10));

        // 5. 选课结果分布（饼图）
        List<Map<String, Object>> enrollDist = new ArrayList<>();
        List<Map<String, Object>> enrolls = dashboardStatMapper.selectEnrollResultDistribution(semesterId);
        if (enrolls != null)
        {
            for (Map<String, Object> row : enrolls)
            {
                Map<String, Object> item = new HashMap<>();
                item.put("name", enrollResultName(row.get("resultStatus")));
                item.put("value", row.get("total"));
                enrollDist.add(item);
            }
        }
        overview.put("enrollDistribution", enrollDist);

        return overview;
    }

    private String levelName(Object level)
    {
        try
        {
            int idx = Integer.parseInt(String.valueOf(level));
            if (idx >= 0 && idx < WARNING_LEVEL_NAMES.length)
            {
                return WARNING_LEVEL_NAMES[idx];
            }
        }
        catch (NumberFormatException ignore)
        {
        }
        return "未知";
    }

    private String enrollResultName(Object status)
    {
        try
        {
            int idx = Integer.parseInt(String.valueOf(status));
            if (idx >= 0 && idx < ENROLL_RESULT_NAMES.length)
            {
                return ENROLL_RESULT_NAMES[idx];
            }
        }
        catch (NumberFormatException ignore)
        {
        }
        return "其他";
    }
}
