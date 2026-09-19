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
import com.yu.sam.mapper.SamDegreeReviewMapper;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.service.ISamDegreeReviewService;

@Service
public class SamDegreeReviewServiceImpl implements ISamDegreeReviewService
{
    private static final Logger log = LoggerFactory.getLogger(SamDegreeReviewServiceImpl.class);

    @Autowired
    private SamDegreeReviewMapper samDegreeReviewMapper;

    @Autowired
    private SamWarningDataMapper samWarningDataMapper;

    /** 学位审核 GPA 最低要求（默认 2.0，可后台配置，S3 基础）。 */
    @Value("${sam.degree.gpaThreshold:2.0}")
    private double gpaThreshold;

    @Override
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.selectSamDegreeReviewByReviewId(reviewId); }
    @Override
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview) { return samDegreeReviewMapper.selectSamDegreeReviewList(samDegreeReview); }
    @Override
    @Transactional
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setCreateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.insertSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setUpdateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.updateSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewId(reviewId); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewIds(reviewIds); }

    /**
     * 自动审核学位资格
     * 校验条件：GPA达标、学位课程（必修）全部合格
     * 数据来源：跨模块查询 aem_grade_record + tpm_course_library
     * 注：论文校验需对接论文管理系统，当前数据模型中无论文表，保留默认合格并记录审核意见
     */
    @Override
    @Transactional
    public SamDegreeReview autoReview(Long studentId)
    {
        // 查询学生GPA（全部学期累计加权）
        Double gpa = samWarningDataMapper.selectStudentGpa(studentId, null);
        // 学位课程（必修课）不及格门数
        Integer degreeFail = samWarningDataMapper.countDegreeCourseFail(studentId, null);

        SamDegreeReview review = new SamDegreeReview();
        review.setStudentId(studentId);
        review.setGpa(gpa != null ? gpa : 0.0);

        // GPA校验（默认要求GPA >= 2.0，可配置）
        boolean gpaQualified = gpa != null && gpa >= gpaThreshold;
        review.setIsGpaQualified(gpaQualified ? "1" : "0");

        // 学位课程校验（必修课程全部通过）
        boolean degreeCourseQualified = degreeFail == null || degreeFail == 0;
        review.setIsDegreeCourseQualified(degreeCourseQualified ? "1" : "0");

        // 论文校验：当前无论文管理模块，默认合格（待对接论文系统后完善）
        review.setIsThesisQualified("1");

        // 综合审核结果
        boolean allQualified = gpaQualified && degreeCourseQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");

        StringBuilder opinion = new StringBuilder();
        if (allQualified) {
            opinion.append("自动审核通过：GPA=").append(gpa).append("，学位课程全部合格");
        } else {
            opinion.append("自动审核不通过：");
            if (!gpaQualified) opinion.append("GPA=").append(gpa != null ? gpa : "无").append("，未达到").append(gpaThreshold).append("要求；");
            if (!degreeCourseQualified) opinion.append("有").append(degreeFail).append("门学位课程（必修）未通过；");
        }
        review.setReviewOpinion(opinion.toString());
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());

        samDegreeReviewMapper.insertSamDegreeReview(review);
        log.info("学生[{}]学位资格自动审核完成：{}", studentId, allQualified ? "通过" : "不通过");
        return review;
    }

    /**
     * 批量自动审核学位资格（S1）。
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
                    SamDegreeReview r = autoReview(sid);
                    if ("1".equals(r.getReviewStatus())) passed++; else rejected++;
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("studentId", sid);
                    d.put("reviewStatus", r.getReviewStatus());
                    d.put("reviewOpinion", r.getReviewOpinion());
                    details.add(d);
                } catch (Exception e) {
                    rejected++;
                    log.error("学生[{}]学位审核异常", sid, e);
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
        log.info("学位批量审核完成：共{}，通过{}，不通过{}", total, passed, rejected);
        return result;
    }
}
