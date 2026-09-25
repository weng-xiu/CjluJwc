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
import com.yu.sam.service.ISamWarningAssistService;
import com.yu.sam.service.ISamWarningRuleConfigService;
import com.yu.sam.service.ISamWarningService;
import com.yu.system.service.ISysNotifyService;

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

    /** S6：预警生成后多渠道通知（站内信+邮件+短信，复用统一通知服务） */
    @Autowired
    private ISysNotifyService sysNotifyService;

    /** S6：预警生成后按级别自动派发帮扶任务 */
    @Autowired
    private ISamWarningAssistService samWarningAssistService;

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

        // S6：生成后自动向学生推送预警消息（多渠道），并按级别派发帮扶任务（异常不影响主流程）
        postProcessWarnings(studentId, warnings);

        return warnings;
    }

    /**
     * S6：预警生成后处理。
     * 1) 向学生合并推送一条多渠道消息（站内信恒发，邮件/短信按开关）；
     * 2) 逐条按级别自动派发帮扶任务（开关与级别门限由 sys_config 控制）。
     * 任一子处理异常均不影响预警主流程。
     */
    private void postProcessWarnings(Long studentId, List<SamWarning> warnings)
    {
        if (warnings == null || warnings.isEmpty())
        {
            return;
        }
        Map<String, Object> contact = null;
        try
        {
            contact = samWarningDataMapper.selectStudentContact(studentId);
        }
        catch (Exception e)
        {
            log.error("查询学生联系信息失败 studentId={}", studentId, e);
        }

        // 1) 多渠道推送给学生
        try
        {
            Object userIdObj = contact == null ? null : contact.get("userId");
            if (userIdObj == null)
            {
                log.info("学生[{}]未关联系统账号，跳过预警消息推送", studentId);
            }
            else
            {
                String[] levelNames = {"一般", "严重", "高危"};
                int maxLevel = 0;
                StringBuilder reasons = new StringBuilder();
                for (SamWarning w : warnings)
                {
                    int lv = 0;
                    try { lv = w.getWarningLevel() == null ? 0 : Integer.parseInt(w.getWarningLevel()); } catch (Exception ignore) { }
                    if (lv > maxLevel) { maxLevel = lv; }
                    if (reasons.length() > 0) { reasons.append("\n"); }
                    reasons.append(w.getWarningReason());
                }
                String levelName = levelNames[Math.min(maxLevel, levelNames.length - 1)];
                String studentName = contact.get("studentName") == null ? "" : String.valueOf(contact.get("studentName"));
                String title = "【学业预警-" + levelName + "】请及时关注自身学业情况";
                String content = studentName + " 同学，本学期学业预警（" + levelName + "）：\n" + reasons
                        + "\n如有疑问请联系辅导员或教务科。";
                sysNotifyService.notifyUser(((Number) userIdObj).longValue(), "0", title, content,
                        "warning", warnings.get(0).getWarningId(), true, true);
            }
        }
        catch (Exception e)
        {
            log.error("预警消息推送失败（studentId={}）", studentId, e);
        }

        // 2) 按级别自动派发帮扶任务
        String studentName = contact == null || contact.get("studentName") == null ? "" : String.valueOf(contact.get("studentName"));
        String studentNo = contact == null || contact.get("studentNo") == null ? "" : String.valueOf(contact.get("studentNo"));
        for (SamWarning w : warnings)
        {
            try
            {
                samWarningAssistService.autoDispatch(w, studentName, studentNo);
            }
            catch (Exception e)
            {
                log.error("预警帮扶自动派发失败 warningId={}", w.getWarningId(), e);
            }
        }
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
