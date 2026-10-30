package com.yu.sam.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.utils.DateUtils;
import com.yu.sam.domain.SamStudentIntervention;
import com.yu.sam.mapper.SamStudentInterventionMapper;
import com.yu.sam.service.ISamStudentInterventionService;

/**
 * 学生干预闭环记录 Service 实现（F4 预警-帮扶-效果回收）。
 *
 * 效果指标按"学生+学期"口径实时计算，不落库：成绩补录/更正后无需重算台账。
 *
 * @author F4
 */
@Service
public class SamStudentInterventionServiceImpl implements ISamStudentInterventionService
{
    @Autowired
    private SamStudentInterventionMapper interventionMapper;

    @Override
    public List<SamStudentIntervention> selectInterventionList(SamStudentIntervention query)
    {
        List<SamStudentIntervention> list = interventionMapper.selectInterventionList(query);
        if (list != null)
        {
            for (SamStudentIntervention record : list)
            {
                enrichEffect(record);
            }
        }
        return list;
    }

    @Override
    public SamStudentIntervention selectInterventionById(Long interventionId)
    {
        SamStudentIntervention record = interventionMapper.selectInterventionById(interventionId);
        enrichEffect(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertIntervention(SamStudentIntervention record)
    {
        record.setCreateTime(DateUtils.getNowDate());
        return interventionMapper.insertIntervention(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateIntervention(SamStudentIntervention record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        return interventionMapper.updateIntervention(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteInterventionByIds(Long[] interventionIds)
    {
        return interventionMapper.deleteInterventionByIds(interventionIds);
    }

    @Override
    public Map<String, Object> previewEffect(Long studentId, Long baselineSemesterId, Long effectSemesterId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        if (studentId == null)
        {
            result.put("available", false);
            result.put("note", "学生ID不能为空。");
            return result;
        }
        Map<String, Object> baseline = baselineSemesterId == null ? null
                : interventionMapper.selectSemesterMetrics(studentId, baselineSemesterId);
        Map<String, Object> effect = effectSemesterId == null ? null
                : interventionMapper.selectSemesterMetrics(studentId, effectSemesterId);
        result.put("available", baseline != null || effect != null);
        result.put("baseline", baseline);
        result.put("effect", effect);
        result.put("gpaDelta", delta(of(baseline, "gpa"), of(effect, "gpa")));
        result.put("passRateDelta", delta(of(baseline, "passRate"), of(effect, "passRate")));
        result.put("note", "效果指标按所选学期现算：GPA 按学分加权，及格率按修读门次；未选学期则该项为空。");
        return result;
    }

    /** 为单条记录填充基线/效果指标与差值。 */
    private void enrichEffect(SamStudentIntervention record)
    {
        if (record == null || record.getStudentId() == null)
        {
            return;
        }
        Map<String, Object> baseline = record.getBaselineSemesterId() == null ? null
                : interventionMapper.selectSemesterMetrics(record.getStudentId(), record.getBaselineSemesterId());
        Map<String, Object> effect = record.getEffectSemesterId() == null ? null
                : interventionMapper.selectSemesterMetrics(record.getStudentId(), record.getEffectSemesterId());
        record.setBaselineGpa(of(baseline, "gpa"));
        record.setEffectGpa(of(effect, "gpa"));
        record.setBaselinePassRate(of(baseline, "passRate"));
        record.setEffectPassRate(of(effect, "passRate"));
        record.setGpaDelta(delta(record.getBaselineGpa(), record.getEffectGpa()));
        record.setPassRateDelta(delta(record.getBaselinePassRate(), record.getEffectPassRate()));
    }

    /** 从指标 Map 中读取数值字段并统一为 Double。 */
    private Double of(Map<String, Object> metrics, String key)
    {
        if (metrics == null || metrics.get(key) == null)
        {
            return null;
        }
        Object value = metrics.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : Double.valueOf(value.toString());
    }

    /** 差值（后者-前者），保留两位小数；任一为空则返回 null。 */
    private Double delta(Double before, Double after)
    {
        if (before == null || after == null)
        {
            return null;
        }
        return Math.round((after - before) * 100d) / 100d;
    }
}
