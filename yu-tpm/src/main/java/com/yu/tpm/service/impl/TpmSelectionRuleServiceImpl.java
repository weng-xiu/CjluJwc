package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.yu.common.utils.DateUtils;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmSelectionRuleMapper;
import com.yu.tpm.service.ITpmSelectionRuleService;

/**
 * 选课规则Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmSelectionRuleServiceImpl implements ITpmSelectionRuleService 
{
    /** 规则类型：专业限制 */
    private static final String RULE_TYPE_MAJOR = "1";
    /** 规则类型：年级限制 */
    private static final String RULE_TYPE_GRADE = "2";
    /** 规则类型：院系限制 */
    private static final String RULE_TYPE_DEPT = "3";
    /** 规则类型：人数上限 */
    private static final String RULE_TYPE_CAPACITY = "4";
    /** 规则类型：先修课程 */
    private static final String RULE_TYPE_PREREQUISITE = "5";

    @Autowired
    private TpmSelectionRuleMapper tpmSelectionRuleMapper;

    @Autowired
    private SamStudentMapper samStudentMapper;

    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Override
    public TpmSelectionRule selectTpmSelectionRuleByRuleId(Long ruleId)
    {
        return tpmSelectionRuleMapper.selectTpmSelectionRuleByRuleId(ruleId);
    }

    @Override
    public List<TpmSelectionRule> selectTpmSelectionRuleList(TpmSelectionRule tpmSelectionRule)
    {
        return tpmSelectionRuleMapper.selectTpmSelectionRuleList(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int insertTpmSelectionRule(TpmSelectionRule tpmSelectionRule)
    {
        tpmSelectionRule.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionRuleMapper.insertTpmSelectionRule(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int updateTpmSelectionRule(TpmSelectionRule tpmSelectionRule)
    {
        tpmSelectionRule.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionRuleMapper.updateTpmSelectionRule(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRuleByRuleId(Long ruleId)
    {
        return tpmSelectionRuleMapper.deleteTpmSelectionRuleByRuleId(ruleId);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRuleByRuleIds(Long[] ruleIds)
    {
        return tpmSelectionRuleMapper.deleteTpmSelectionRuleByRuleIds(ruleIds);
    }

    /**
     * 规则引擎：校验学生选课时是否违反该轮次下启用的规则
     * 学生信息来源：yu-sam 的 SamStudentMapper.selectSamStudentByStudentId（含 majorId/deptId/enrollmentYear）
     */
    @Override
    public List<String> validate(Long roundId, Long studentId, Long courseOfferingId)
    {
        List<String> violations = new ArrayList<>();

        // 查询轮次下启用的规则（status='0'），按 priority 排序
        TpmSelectionRule query = new TpmSelectionRule();
        query.setRoundId(roundId);
        query.setStatus("0");
        List<TpmSelectionRule> rules = tpmSelectionRuleMapper.selectTpmSelectionRuleList(query);
        if (rules == null || rules.isEmpty())
        {
            return violations;
        }

        // 获取学生信息
        SamStudent student = samStudentMapper.selectSamStudentByStudentId(studentId);

        for (TpmSelectionRule rule : rules)
        {
            String ruleType = rule.getRuleType();
            String restrictValue = rule.getRestrictValue();
            if (restrictValue == null || restrictValue.trim().isEmpty())
            {
                continue;
            }
            restrictValue = restrictValue.trim();

            if (RULE_TYPE_MAJOR.equals(ruleType))
            {
                if (student == null || student.getMajorId() == null
                        || !parseIdList(restrictValue).contains(student.getMajorId()))
                {
                    violations.add(rule.getRuleName() + "：该课程仅限指定专业学生选修");
                }
            }
            else if (RULE_TYPE_GRADE.equals(ruleType))
            {
                if (student == null || student.getEnrollmentYear() == null
                        || !restrictValue.equals(student.getEnrollmentYear().trim()))
                {
                    violations.add(rule.getRuleName() + "：该课程仅限" + restrictValue + "级学生选修");
                }
            }
            else if (RULE_TYPE_DEPT.equals(ruleType))
            {
                if (student == null || student.getDeptId() == null
                        || !parseIdList(restrictValue).contains(student.getDeptId()))
                {
                    violations.add(rule.getRuleName() + "：该课程仅限指定院系学生选修");
                }
            }
            else if (RULE_TYPE_CAPACITY.equals(ruleType))
            {
                try
                {
                    int limit = Integer.parseInt(restrictValue);
                    int enrolled = tpmSelectionEnrollmentMapper.selectCountByOffering(courseOfferingId);
                    if (enrolled >= limit)
                    {
                        violations.add(rule.getRuleName() + "：该课程规则容量已满（上限" + limit + "人）");
                    }
                }
                catch (NumberFormatException e)
                {
                    // 配置非法，跳过该规则
                }
            }
            else if (RULE_TYPE_PREREQUISITE.equals(ruleType))
            {
                List<Long> prerequisiteCourseIds = parseLongList(restrictValue);
                if (!prerequisiteCourseIds.isEmpty())
                {
                    int completed = tpmSelectionEnrollmentMapper.countCompletedCourses(studentId, prerequisiteCourseIds);
                    if (completed < prerequisiteCourseIds.size())
                    {
                        violations.add(rule.getRuleName() + "：未完成先修课程");
                    }
                }
            }
        }
        return violations;
    }

    /**
     * 将逗号分隔的ID字符串解析为Long列表
     */
    private List<Long> parseIdList(String value)
    {
        return parseLongList(value);
    }

    private List<Long> parseLongList(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return new ArrayList<>();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s ->
                {
                    try
                    {
                        return Long.parseLong(s);
                    }
                    catch (NumberFormatException e)
                    {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }
}
