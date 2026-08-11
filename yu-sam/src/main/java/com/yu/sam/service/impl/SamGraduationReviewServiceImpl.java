package com.yu.sam.service.impl;

import java.util.Date;
import java.util.List;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 数据来源：跨模块查询 aem_grade_record + tpm_course_library
     */
    @Override
    @Transactional
    public SamGraduationReview autoReview(Long studentId)
    {
        // 跨学期累计：统计全部学期的学分和不及格课程
        Double earnedCredits = samWarningDataMapper.selectStudentEarnedCredits(studentId, null);
        Double requiredCredits = samWarningDataMapper.selectStudentRequiredCredits(studentId, null);
        Integer failCount = samWarningDataMapper.selectStudentFailCourseCount(studentId, null);
        // 英语课程：课程名包含"英语"且未通过的门数
        Integer englishFail = samWarningDataMapper.countFailByCourseNameKeyword(studentId, null, "英语");
        // 体育课程：课程名包含"体育"且未通过的门数
        Integer peFail = samWarningDataMapper.countFailByCourseNameKeyword(studentId, null, "体育");

        SamGraduationReview review = new SamGraduationReview();
        review.setStudentId(studentId);
        review.setTotalCreditsEarned(earnedCredits != null ? earnedCredits : 0.0);
        review.setRequiredCredits(requiredCredits != null ? requiredCredits : 0.0);

        // 学分校验
        boolean creditQualified = earnedCredits != null && requiredCredits != null
                && earnedCredits >= requiredCredits;
        review.setIsCreditQualified(creditQualified ? "1" : "0");

        // 课程校验（无不及格课程）
        boolean courseQualified = failCount == null || failCount == 0;
        review.setIsCourseQualified(courseQualified ? "1" : "0");

        // 英语校验（所有英语课程均通过）
        boolean englishQualified = englishFail == null || englishFail == 0;
        review.setIsEnglishQualified(englishQualified ? "1" : "0");

        // 体育校验（所有体育课程均通过）
        boolean peQualified = peFail == null || peFail == 0;
        review.setIsPeQualified(peQualified ? "1" : "0");

        // 综合审核结果
        boolean allQualified = creditQualified && courseQualified && englishQualified && peQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");

        StringBuilder opinion = new StringBuilder();
        if (allQualified) {
            opinion.append("自动审核通过：学分达标").append(earnedCredits).append("/").append(requiredCredits)
                   .append("，无不及格课程，英语/体育合格");
        } else {
            opinion.append("自动审核不通过：");
            if (!creditQualified) opinion.append("学分不达标(").append(earnedCredits).append("/").append(requiredCredits).append(")；");
            if (!courseQualified) opinion.append("有").append(failCount).append("门不及格课程；");
            if (!englishQualified) opinion.append("英语课程未通过；");
            if (!peQualified) opinion.append("体育课程未通过；");
        }
        review.setReviewOpinion(opinion.toString());
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());

        samGraduationReviewMapper.insertSamGraduationReview(review);
        log.info("学生[{}]毕业资格自动审核完成：{}", studentId, allQualified ? "通过" : "不通过");
        return review;
    }
}
