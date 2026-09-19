package com.yu.sam.service.impl;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamGraduationReviewMapper;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.domain.SamGraduationReview;
import com.yu.sam.service.ISamGraduationReviewService;

@Service
public class SamGraduationReviewServiceImpl implements ISamGraduationReviewService
{
    private static final Logger log = LoggerFactory.getLogger(SamGraduationReviewServiceImpl.class);

    @Autowired
    private SamGraduationReviewMapper samGraduationReviewMapper;

    @Autowired
    private SamWarningDataMapper samWarningDataMapper;

    /** 是否启用"培养方案学分结构分项比对"口径（数据治理完成后开启；默认关闭以保留旧口径，规避历史结论突变风险）。 */
    @Value("${sam.review.usePlanCredit:false}")
    private boolean usePlanCredit;

    @Override
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId) { return samGraduationReviewMapper.selectSamGraduationReviewByReviewId(reviewId); }
    @Override
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview) { return samGraduationReviewMapper.selectSamGraduationReviewList(samGraduationReview); }
    @Override
    @Transactional
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview) { samGraduationReview.setCreateTime(DateUtils.getNowDate()); return samGraduationReviewMapper.insertSamGraduationReview(samGraduationReview); }
    @Override
    @Transactional
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview) { samGraduationReview.setUpdateTime(DateUtils.getNowDate()); return samGraduationReviewMapper.updateSamGraduationReview(samGraduationReview); }
    @Override
    @Transactional
    public int deleteSamGraduationReviewByReviewId(Long reviewId) { return samGraduationReviewMapper.deleteSamGraduationReviewByReviewId(reviewId); }
    @Override
    @Transactional
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds) { return samGraduationReviewMapper.deleteSamGraduationReviewByReviewIds(reviewIds); }

    /**
     * 自动审核毕业资格
     * 校验条件：学分达标、无不及格课程、英语合格、体育合格
     * 数据来源：跨模块查询 aem_grade_record + tpm_course_library + tpm_training_plan/tpm_credit_structure
     *
     * <p>S2 判定修正：
     * <ul>
     *   <li>应修学分优先取学生所属培养方案规定学分（total_credits / 学分结构汇总），旧口径（已修学分之和）作为降级；</li>
     *   <li>英语/体育采用课程属性标记（course_category=FOREIGN_LANGUAGE/PE）判定，属性未完成治理时降级为课程名关键字，两者均无该门课程时视为无该项要求（合格）。</li>
     * </ul>
     */
    @Override
    @Transactional
    public SamGraduationReview autoReview(Long studentId)
    {
        // 跨学期累计：统计全部学期的学分和不及格课程
        Double earnedCredits = samWarningDataMapper.selectStudentEarnedCredits(studentId, null);
        Integer failCount = samWarningDataMapper.selectStudentFailCourseCount(studentId, null);

        // === S2-1：应修学分口径（培养方案优先，降级为旧口径） ===
        Long planId = samWarningDataMapper.selectPlanIdByStudent(studentId);
        Double requiredCredits;
        String creditSource;
        if (usePlanCredit && planId != null) {
            Double planReq = samWarningDataMapper.selectPlanRequiredCredits(planId);
            if (planReq != null && planReq > 0) {
                requiredCredits = planReq;
                creditSource = "培养方案(planId=" + planId + ")";
            } else {
                requiredCredits = samWarningDataMapper.selectStudentRequiredCredits(studentId, null);
                creditSource = "降级-已修学分之和(方案未配置应修学分)";
            }
        } else {
            requiredCredits = samWarningDataMapper.selectStudentRequiredCredits(studentId, null);
            creditSource = usePlanCredit ? "降级-已修学分之和(无匹配方案)" : "旧口径-已修学分之和";
        }

        // === S2-2：英语/体育采用课程属性标记判定，属性缺失时降级关键字 ===
        Integer englishFail = countFailByAttributeOrKeyword(studentId, "FOREIGN_LANGUAGE", "英语");
        Integer peFail = countFailByAttributeOrKeyword(studentId, "PE", "体育");

        double earned = earnedCredits != null ? earnedCredits : 0.0;
        double required = requiredCredits != null ? requiredCredits : 0.0;

        SamGraduationReview review = new SamGraduationReview();
        review.setStudentId(studentId);
        review.setTotalCreditsEarned(earned);
        review.setRequiredCredits(required);

        // 学分校验
        boolean creditQualified = earned >= required;
        review.setIsCreditQualified(creditQualified ? "1" : "0");

        // 课程校验（无不及格课程）
        boolean courseQualified = failCount == null || failCount == 0;
        review.setIsCourseQualified(courseQualified ? "1" : "0");

        // 英语校验（所有英语类课程均通过）
        boolean englishQualified = englishFail == null || englishFail == 0;
        review.setIsEnglishQualified(englishQualified ? "1" : "0");

        // 体育校验（所有体育类课程均通过）
        boolean peQualified = peFail == null || peFail == 0;
        review.setIsPeQualified(peQualified ? "1" : "0");

        // 综合审核结果
        boolean allQualified = creditQualified && courseQualified && englishQualified && peQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");

        StringBuilder opinion = new StringBuilder();
        opinion.append("[").append(creditSource).append("] ");
        if (allQualified) {
            opinion.append("自动审核通过：学分达标").append(earned).append("/").append(required)
                   .append("，无不及格课程，英语/体育合格");
        } else {
            opinion.append("自动审核不通过：");
            if (!creditQualified) opinion.append("学分不达标(").append(earned).append("/").append(required).append(")；");
            if (!courseQualified) opinion.append("有").append(failCount).append("门不及格课程；");
            if (!englishQualified) opinion.append("英语课程未通过；");
            if (!peQualified) opinion.append("体育课程未通过；");
        }
        // 启用培养方案口径时附分项达成情况（为学生预审/差距清单提供依据）
        if (usePlanCredit && planId != null) {
            String breakdown = buildCreditSectionBreakdown(studentId, planId);
            if (breakdown != null && !breakdown.isEmpty()) {
                opinion.append(" 分项：").append(breakdown);
            }
        }
        review.setReviewOpinion(opinion.toString());
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());

        samGraduationReviewMapper.insertSamGraduationReview(review);
        log.info("学生[{}]毕业资格自动审核完成：{}", studentId, allQualified ? "通过" : "不通过");
        return review;
    }

    /**
     * 批量自动审核（S1：一键审核全年级/多学生）。
     * 每个学生独立执行审核并落库，单个失败不影响其他学生。
     *
     * @param studentIds 学生ID列表
     * @return 汇总结果（total/passed/rejected/failList）
     */
    @Override
    @Transactional
    public Map<String, Object> batchAutoReview(List<Long> studentIds)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        int total = 0, passed = 0, rejected = 0;
        if (studentIds != null) {
            for (Long sid : studentIds) {
                if (sid == null) continue;
                total++;
                try {
                    SamGraduationReview r = autoReview(sid);
                    if ("1".equals(r.getReviewStatus())) passed++; else rejected++;
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("studentId", sid);
                    d.put("reviewStatus", r.getReviewStatus());
                    d.put("reviewOpinion", r.getReviewOpinion());
                    details.add(d);
                } catch (Exception e) {
                    rejected++;
                    log.error("学生[{}]毕业审核异常", sid, e);
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("studentId", sid);
                    d.put("reviewStatus", "error");
                    d.put("reviewOpinion", e.getMessage());
                    details.add(d);
                }
            }
        }
        result.put("total", total);
        result.put("passed", passed);
        result.put("rejected", rejected);
        result.put("details", details);
        log.info("毕业批量审核完成：共{}，通过{}，不通过{}", total, passed, rejected);
        return result;
    }

    /**
     * 按课程属性统计未通过门数；属性未治理时降级为课程名关键字。
     * 若学生既无属性标记课程也无关键字课程，返回 0（视为无该项要求）。
     */
    private Integer countFailByAttributeOrKeyword(Long studentId, String category, String keyword) {
        Integer tagged = samWarningDataMapper.countCoursesByCourseCategory(studentId, null, category);
        if (tagged != null && tagged > 0) {
            return samWarningDataMapper.countFailByCourseCategory(studentId, null, category);
        }
        Integer kwCourses = samWarningDataMapper.countCoursesByNameKeyword(studentId, null, keyword);
        if (kwCourses != null && kwCourses > 0) {
            return samWarningDataMapper.countFailByCourseNameKeyword(studentId, null, keyword);
        }
        return 0;
    }

    /**
     * 拼接培养方案学分结构分项达成情况（已获/要求）。
     */
    private String buildCreditSectionBreakdown(Long studentId, Long planId) {
        List<Map<String, Object>> sections = samWarningDataMapper.selectPlanCreditSections(planId);
        if (sections == null || sections.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> s : sections) {
            String category = s.get("creditType") != null ? s.get("creditType").toString() : null;
            String name = s.get("creditTypeName") != null ? s.get("creditTypeName").toString() : category;
            double req = s.get("requiredCredit") != null ? Double.parseDouble(s.get("requiredCredit").toString()) : 0.0;
            Double e = samWarningDataMapper.sumEarnedCreditByCourseCategory(studentId, null, category);
            double earnedSec = e != null ? e : 0.0;
            sb.append(name).append(":").append(earnedSec).append("/").append(req)
              .append(earnedSec >= req ? "✓" : "✗").append(" ");
        }
        return sb.toString().trim();
    }
}
