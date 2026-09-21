package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmResourceStatMapper;
import com.yu.brm.service.IBrmResourceStatService;

/**
 * 资源利用分析Service业务层处理（B2）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Service
public class BrmResourceStatServiceImpl implements IBrmResourceStatService
{
    private static final Logger log = LoggerFactory.getLogger(BrmResourceStatServiceImpl.class);

    @Autowired
    private BrmResourceStatMapper brmResourceStatMapper;

    /** 每天可用节次数（用于教室周网格分母），默认8 */
    @Value("${brm.resource.periodsPerDay:8}")
    private int periodsPerDay;

    /** 每周排课工作日数，默认5 */
    @Value("${brm.resource.workingDays:5}")
    private int workingDays;

    /** 维保到期阈值天数，默认180 */
    @Value("${brm.maintenance.dueDays:180}")
    private int maintenanceDueDays;

    @Override
    public Map<String, Object> overview(Long semesterId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rooms = classroomUtilization(semesterId, null);
        int roomTotal = rooms.size();
        int roomUsed = 0;
        long sessionSum = 0;
        for (Map<String, Object> r : rooms)
        {
            long ws = toLong(r.get("weeklySessions"));
            sessionSum += ws;
            if (ws > 0) roomUsed++;
        }
        double avgRate = (roomTotal > 0 && periodsPerDay > 0 && workingDays > 0)
                ? round(sessionSum * 100.0 / ((double) roomTotal * periodsPerDay * workingDays)) : 0.0;
        Map<String, Object> classroom = new LinkedHashMap<>();
        classroom.put("total", roomTotal);
        classroom.put("used", roomUsed);
        classroom.put("idle", roomTotal - roomUsed);
        classroom.put("avgUtilizationRate", avgRate);
        result.put("classroom", classroom);

        // 设备状态
        List<Map<String, Object>> eq = brmResourceStatMapper.equipmentStatusSummary();
        int eqTotal = 0; int eqNormal = 0; int eqDisabled = 0;
        if (eq != null)
        {
            for (Map<String, Object> e : eq)
            {
                String st = str(e.get("status"));
                int c = (int) toLong(e.get("cnt"));
                eqTotal += c;
                if ("0".equals(st)) eqNormal += c;
                else if ("1".equals(st)) eqDisabled += c;
            }
        }
        Map<String, Object> equipment = new LinkedHashMap<>();
        equipment.put("total", eqTotal);
        equipment.put("normal", eqNormal);
        equipment.put("disabled", eqDisabled);
        equipment.put("dueCount", maintenanceDue().size());
        result.put("equipment", equipment);

        // 教师工作量
        List<Map<String, Object>> teachers = teacherWorkload(semesterId);
        int teacherTotal = teachers.size();
        int teacherActive = 0; long tSessionSum = 0;
        for (Map<String, Object> t : teachers)
        {
            long ws = toLong(t.get("weeklySessions"));
            tSessionSum += ws;
            if (ws > 0) teacherActive++;
        }
        Map<String, Object> teacher = new LinkedHashMap<>();
        teacher.put("total", teacherTotal);
        teacher.put("active", teacherActive);
        teacher.put("avgWeeklySessions", teacherActive > 0 ? round((double) tSessionSum / teacherActive) : 0.0);
        result.put("teacher", teacher);
        return result;
    }

    @Override
    public List<Map<String, Object>> classroomUtilization(Long semesterId, Long buildingId)
    {
        List<Map<String, Object>> list = brmResourceStatMapper.classroomUtilization(semesterId, buildingId);
        if (list == null)
        {
            return new ArrayList<>();
        }
        double denom = (double) periodsPerDay * workingDays;
        for (Map<String, Object> r : list)
        {
            long ws = toLong(r.get("weeklySessions"));
            double rate = denom > 0 ? round(ws * 100.0 / denom) : 0.0;
            r.put("utilizationRate", rate);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> teacherWorkload(Long semesterId)
    {
        List<Map<String, Object>> list = brmResourceStatMapper.teacherWorkload(semesterId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public List<Map<String, Object>> maintenanceDue()
    {
        List<Map<String, Object>> list = brmResourceStatMapper.maintenanceDueList(maintenanceDueDays);
        return list == null ? new ArrayList<>() : list;
    }

    private double round(double v)
    {
        return Math.round(v * 100.0) / 100.0;
    }

    private long toLong(Object o)
    {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private String str(Object o)
    {
        return o == null ? null : o.toString();
    }
}
