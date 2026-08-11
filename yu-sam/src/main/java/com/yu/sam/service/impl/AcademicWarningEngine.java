package com.yu.sam.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.domain.SamWarningRuleConfig;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.service.ISamWarningRuleConfigService;
import com.yu.sam.service.ISamWarningService;

/**
 * 学业预警生成引擎
 * 根据预警规则配置，自动评估学生学业数据并生成预警记录
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class AcademicWarningEngine
{
    private static final Logger log = LoggerFactory.getLogger(AcademicWarningEngine.class);

    /** 每批处理学生数量 */
    private static final int BATCH_SIZE = 1000;

    @Autowired
    private ISamWarningService samWarningService;

    @Autowired
    private ISamWarningRuleConfigService warningRuleConfigService;

    @Autowired
    private SamWarningDataMapper samWarningDataMapper;

    @Autowired
    private com.yu.sam.mapper.SamWarningMapper samWarningMapper;

    /**
     * 为指定学生生成预警。
     * 生成前先清理该学生该学期的"未解除"旧预警，避免重复批量生成时产生重复记录。
     *
     * @param studentId 学生ID
     * @param semesterId 学期ID
     * @return 生成的预警列表
     */
    @Transactional
    public List<SamWarning> generateWarningsForStudent(Long studentId, Long semesterId)
    {
        // 清理该学生该学期未解除的旧预警（联动去重）
        samWarningMapper.deleteActiveWarningsByStudentAndSemester(studentId, semesterId);

        List<SamWarning> warnings = new ArrayList<>();
        List<SamWarningRuleConfig> rules = warningRuleConfigService.selectEnabledRules();

        for (SamWarningRuleConfig rule : rules)
        {
            SamWarning warning = evaluateRule(studentId, semesterId, rule);
            if (warning != null)
            {
                warnings.add(warning);
            }
        }

        // 综合预警：多个维度同时触发时
        if (warnings.size() >= 2)
        {
            SamWarning comprehensive = new SamWarning();
            comprehensive.setStudentId(studentId);
            comprehensive.setSemesterId(semesterId);
            comprehensive.setWarningType("3"); // 综合
            comprehensive.setWarningLevel("2"); // 高危
            comprehensive.setWarningReason("多维度预警同时触发：" + warnings.stream()
                    .map(SamWarning::getWarningReason).collect(Collectors.joining("；")));
            comprehensive.setWarningDate(new Date());
            warnings.add(comprehensive);
        }

        // 保存预警记录
        for (SamWarning w : warnings)
        {
            samWarningService.insertSamWarning(w);
        }

        return warnings;
    }

    /**
     * 批量生成预警（按学院分批）
     * 
     * @param semesterId 学期ID
     * @return 统计结果
     */
    public Map<String, Object> generateWarningsBatch(Long semesterId)
    {
        Map<String, Object> result = new HashMap<>();
        int totalStudents = samWarningDataMapper.selectStudentCount();
        int totalWarnings = 0;
        int warningStudents = 0;
        int errorCount = 0;

        log.info("开始批量生成学业预警，学期ID：{}，学生总数：{}", semesterId, totalStudents);

        int offset = 0;
        while (offset < totalStudents)
        {
            List<Long> studentIds = samWarningDataMapper.selectAllStudentIds(offset, BATCH_SIZE);
            if (studentIds == null || studentIds.isEmpty())
            {
                break;
            }

            for (Long studentId : studentIds)
            {
                try
                {
                    List<SamWarning> warnings = generateWarningsForStudent(studentId, semesterId);
                    if (!warnings.isEmpty())
                    {
                        totalWarnings += warnings.size();
                        warningStudents++;
                    }
                }
                catch (Exception e)
                {
                    errorCount++;
                    log.error("学生ID {} 预警生成失败：{}", studentId, e.getMessage());
                }
            }

            offset += BATCH_SIZE;
            log.info("已处理 {}/{} 名学生", Math.min(offset, totalStudents), totalStudents);
        }

        result.put("totalStudents", totalStudents);
        result.put("totalWarnings", totalWarnings);
        result.put("warningStudents", warningStudents);
        result.put("errorCount", errorCount);
        result.put("semesterId", semesterId);

        log.info("学业预警批量生成完成，统计：{}", result);
        return result;
    }

    /**
     * 评估单条规则
     * 
     * @param studentId 学生ID
     * @param semesterId 学期ID
     * @param rule 预警规则
     * @return 预警对象（null表示未触发）
     */
    private SamWarning evaluateRule(Long studentId, Long semesterId, SamWarningRuleConfig rule)
    {
        String ruleCode = rule.getRuleCode();
        switch (ruleCode)
        {
            case "GPA_LOW":
                return evaluateGpaRule(studentId, semesterId, rule);
            case "CREDIT_LOW":
                return evaluateCreditRule(studentId, semesterId, rule);
            case "ATTENDANCE_LOW":
                return evaluateAttendanceRule(studentId, semesterId, rule);
            default:
                log.warn("未知规则代码：{}", ruleCode);
                return null;
        }
    }

    /**
     * 评估GPA规则
     */
    private SamWarning evaluateGpaRule(Long studentId, Long semesterId, SamWarningRuleConfig rule)
    {
        Double gpa = samWarningDataMapper.selectStudentGpa(studentId, semesterId);
        if (gpa == null)
        {
            return null; // 无成绩数据
        }

        double threshold = Double.parseDouble(rule.getThresholdValue());
        if (gpa < threshold)
        {
            SamWarning warning = new SamWarning();
            warning.setStudentId(studentId);
            warning.setSemesterId(semesterId);
            warning.setWarningType(rule.getWarningType());
            warning.setWarningLevel(rule.getWarningLevel());
            String reason = rule.getMessageTemplate()
                    .replace("{gpa}", String.valueOf(gpa))
                    .replace("{threshold}", rule.getThresholdValue());
            warning.setWarningReason(reason);
            warning.setWarningDate(new Date());
            return warning;
        }
        return null;
    }

    /**
     * 评估学分规则
     */
    private SamWarning evaluateCreditRule(Long studentId, Long semesterId, SamWarningRuleConfig rule)
    {
        Double earnedCredits = samWarningDataMapper.selectStudentEarnedCredits(studentId, semesterId);
        Double requiredCredits = samWarningDataMapper.selectStudentRequiredCredits(studentId, semesterId);

        if (requiredCredits == null || requiredCredits == 0)
        {
            return null; // 无课程数据
        }

        double percent = (earnedCredits / requiredCredits) * 100;
        String thresholdStr = rule.getThresholdValue().replace("%", "");
        double threshold = Double.parseDouble(thresholdStr);

        if (percent < threshold)
        {
            SamWarning warning = new SamWarning();
            warning.setStudentId(studentId);
            warning.setSemesterId(semesterId);
            warning.setWarningType(rule.getWarningType());
            warning.setWarningLevel(rule.getWarningLevel());
            String reason = rule.getMessageTemplate()
                    .replace("{earned}", String.valueOf(earnedCredits))
                    .replace("{required}", String.valueOf(requiredCredits))
                    .replace("{threshold}", rule.getThresholdValue());
            warning.setWarningReason(reason);
            warning.setWarningDate(new Date());
            return warning;
        }
        return null;
    }

    /**
     * 评估出勤预警
     * 以不及格课程数作为出勤问题的参考指标
     * 不及格课程数达到阈值时触发预警
     */
    private SamWarning evaluateAttendanceRule(Long studentId, Long semesterId, SamWarningRuleConfig rule)
    {
        Integer failCount = samWarningDataMapper.selectStudentFailCourseCount(studentId, semesterId);
        if (failCount == null || failCount == 0)
        {
            return null;
        }
        double threshold = Double.parseDouble(rule.getThresholdValue());
        if (failCount >= threshold)
        {
            SamWarning warning = new SamWarning();
            warning.setStudentId(studentId);
            warning.setSemesterId(semesterId);
            warning.setWarningType(rule.getWarningType());
            warning.setWarningLevel(rule.getWarningLevel());
            String reason = rule.getMessageTemplate()
                    .replace("{failCount}", String.valueOf(failCount))
                    .replace("{threshold}", rule.getThresholdValue());
            warning.setWarningReason(reason);
            warning.setWarningDate(new Date());
            return warning;
        }
        return null;
    }
}
