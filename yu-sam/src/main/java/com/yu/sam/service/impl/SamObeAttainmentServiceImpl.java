package com.yu.sam.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.utils.DateUtils;
import com.yu.sam.domain.SamObeCourseSupport;
import com.yu.sam.domain.SamObeGraduationRequirement;
import com.yu.sam.domain.SamObeIndicatorPoint;
import com.yu.sam.domain.dto.ObeAttainmentVo;
import com.yu.sam.domain.dto.ObeCourseScore;
import com.yu.sam.mapper.SamObeCourseSupportMapper;
import com.yu.sam.mapper.SamObeDataMapper;
import com.yu.sam.mapper.SamObeGraduationRequirementMapper;
import com.yu.sam.mapper.SamObeIndicatorPointMapper;
import com.yu.sam.obe.ObeAttainmentCalculator;
import com.yu.sam.service.ISamObeAttainmentService;

/**
 * N1 OBE 达成度服务实现。
 *
 * @author N1
 */
@Service
public class SamObeAttainmentServiceImpl implements ISamObeAttainmentService
{
    @Autowired
    private SamObeGraduationRequirementMapper requirementMapper;

    @Autowired
    private SamObeIndicatorPointMapper indicatorMapper;

    @Autowired
    private SamObeCourseSupportMapper supportMapper;

    @Autowired
    private SamObeDataMapper dataMapper;

    /** 纯逻辑计算器（非 Spring bean，便于单测直接实例化）。 */
    private final ObeAttainmentCalculator calculator = new ObeAttainmentCalculator();

    // ---------- 毕业要求 ----------
    @Override
    public List<SamObeGraduationRequirement> selectRequirementList(SamObeGraduationRequirement query)
    {
        return requirementMapper.selectSamObeGraduationRequirementList(query);
    }

    @Override
    public SamObeGraduationRequirement selectRequirementByGrId(Long grId)
    {
        return requirementMapper.selectSamObeGraduationRequirementByGrId(grId);
    }

    @Override
    @Transactional
    public int insertRequirement(SamObeGraduationRequirement record)
    {
        record.setCreateTime(DateUtils.getNowDate());
        return requirementMapper.insertSamObeGraduationRequirement(record);
    }

    @Override
    @Transactional
    public int updateRequirement(SamObeGraduationRequirement record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        return requirementMapper.updateSamObeGraduationRequirement(record);
    }

    @Override
    @Transactional
    public int deleteRequirementByGrIds(Long[] grIds)
    {
        if (grIds == null || grIds.length == 0)
        {
            return 0;
        }
        // 级联软删：毕业要求 → 指标点 → 课程支撑
        List<Long> ipIdList = new ArrayList<>();
        for (Long grId : grIds)
        {
            for (SamObeIndicatorPoint ip : indicatorMapper.selectByGrId(grId))
            {
                ipIdList.add(ip.getIpId());
            }
        }
        if (!ipIdList.isEmpty())
        {
            supportMapper.deleteByIpIds(ipIdList.toArray(new Long[0]));
        }
        indicatorMapper.deleteByGrIds(grIds);
        return requirementMapper.deleteSamObeGraduationRequirementByGrIds(grIds);
    }

    // ---------- 指标点 ----------
    @Override
    public List<SamObeIndicatorPoint> selectIndicatorList(SamObeIndicatorPoint query)
    {
        return indicatorMapper.selectSamObeIndicatorPointList(query);
    }

    @Override
    public SamObeIndicatorPoint selectIndicatorByIpId(Long ipId)
    {
        return indicatorMapper.selectSamObeIndicatorPointByIpId(ipId);
    }

    @Override
    @Transactional
    public int insertIndicator(SamObeIndicatorPoint record)
    {
        if (record.getPlanId() == null && record.getGrId() != null)
        {
            SamObeGraduationRequirement gr = requirementMapper.selectSamObeGraduationRequirementByGrId(record.getGrId());
            if (gr != null)
            {
                record.setPlanId(gr.getPlanId());
            }
        }
        record.setCreateTime(DateUtils.getNowDate());
        return indicatorMapper.insertSamObeIndicatorPoint(record);
    }

    @Override
    @Transactional
    public int updateIndicator(SamObeIndicatorPoint record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        return indicatorMapper.updateSamObeIndicatorPoint(record);
    }

    @Override
    @Transactional
    public int deleteIndicatorByIpIds(Long[] ipIds)
    {
        if (ipIds == null || ipIds.length == 0)
        {
            return 0;
        }
        supportMapper.deleteByIpIds(ipIds);
        return indicatorMapper.deleteSamObeIndicatorPointByIpIds(ipIds);
    }

    // ---------- 课程支撑矩阵 ----------
    @Override
    public List<SamObeCourseSupport> selectSupportList(SamObeCourseSupport query)
    {
        return supportMapper.selectSamObeCourseSupportList(query);
    }

    @Override
    public SamObeCourseSupport selectSupportBySupportId(Long supportId)
    {
        return supportMapper.selectSamObeCourseSupportBySupportId(supportId);
    }

    @Override
    @Transactional
    public int insertSupport(SamObeCourseSupport record)
    {
        if (record.getPlanId() == null && record.getIpId() != null)
        {
            SamObeIndicatorPoint ip = indicatorMapper.selectSamObeIndicatorPointByIpId(record.getIpId());
            if (ip != null)
            {
                record.setPlanId(ip.getPlanId());
            }
        }
        record.setCreateTime(DateUtils.getNowDate());
        return supportMapper.insertSamObeCourseSupport(record);
    }

    @Override
    @Transactional
    public int updateSupport(SamObeCourseSupport record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        return supportMapper.updateSamObeCourseSupport(record);
    }

    @Override
    @Transactional
    public int deleteSupportBySupportIds(Long[] supportIds)
    {
        if (supportIds == null || supportIds.length == 0)
        {
            return 0;
        }
        return supportMapper.deleteSamObeCourseSupportBySupportIds(supportIds);
    }

    // ---------- 达成度计算 ----------
    @Override
    public List<ObeAttainmentVo> calculateByStudent(Long planId, Long studentId)
    {
        Map<Long, Double> scoreMap = buildScoreMap(dataMapper.selectStudentCourseScores(studentId));
        return compute(planId, scoreMap, "student", studentId);
    }

    @Override
    public List<ObeAttainmentVo> calculateCohort(Long planId, Long semesterId)
    {
        Map<Long, Double> scoreMap = buildScoreMap(dataMapper.selectCourseAverageScores(semesterId));
        return compute(planId, scoreMap, "cohort", null);
    }

    @Override
    public Map<String, Object> dashboard(Long planId, Long semesterId)
    {
        List<ObeAttainmentVo> list = calculateCohort(planId, semesterId);
        int total = list.size();
        int achieved = 0;
        double sum = 0.0;
        for (ObeAttainmentVo vo : list)
        {
            if ("1".equals(vo.getAchieved()))
            {
                achieved++;
            }
            sum += vo.getAttainment() == null ? 0 : vo.getAttainment();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", planId);
        result.put("totalRequirements", total);
        result.put("achievedRequirements", achieved);
        result.put("achievedRate", total == 0 ? 0.0 : ObeAttainmentCalculator.round4((double) achieved / total));
        result.put("averageAttainment", total == 0 ? 0.0 : ObeAttainmentCalculator.round4(sum / total));
        result.put("requirements", list);
        return result;
    }

    /** 依据配置与成绩映射，逐毕业要求计算达成度。 */
    private List<ObeAttainmentVo> compute(Long planId, Map<Long, Double> scoreMap, String scope, Long studentId)
    {
        List<ObeAttainmentVo> result = new ArrayList<>();
        List<SamObeGraduationRequirement> grs = requirementMapper.selectByPlanId(planId);
        if (grs == null || grs.isEmpty())
        {
            return result;
        }
        List<SamObeIndicatorPoint> allIps = indicatorMapper.selectByPlanId(planId);
        List<SamObeCourseSupport> allSupports = supportMapper.selectByPlanId(planId);
        Map<Long, String> nameMap = buildNameMap();
        for (SamObeGraduationRequirement gr : grs)
        {
            List<SamObeIndicatorPoint> ipsOfGr = new ArrayList<>();
            for (SamObeIndicatorPoint ip : allIps)
            {
                if (gr.getGrId().equals(ip.getGrId()))
                {
                    ipsOfGr.add(ip);
                }
            }
            ObeAttainmentVo vo = calculator.computeRequirement(gr, ipsOfGr, allSupports, scoreMap, nameMap, scope);
            vo.setStudentId(studentId);
            result.add(vo);
        }
        return result;
    }

    private Map<Long, Double> buildScoreMap(List<ObeCourseScore> scores)
    {
        Map<Long, Double> map = new HashMap<>();
        if (scores != null)
        {
            for (ObeCourseScore s : scores)
            {
                if (s.getCourseId() != null && s.getScore() != null)
                {
                    map.put(s.getCourseId(), s.getScore());
                }
            }
        }
        return map;
    }

    private Map<Long, String> buildNameMap()
    {
        Map<Long, String> map = new HashMap<>();
        for (Map<String, Object> row : dataMapper.selectCourseNames())
        {
            Object id = row.get("courseId");
            Object name = row.get("courseName");
            if (id != null)
            {
                map.put(((Number) id).longValue(), name == null ? null : name.toString());
            }
        }
        return map;
    }
}
