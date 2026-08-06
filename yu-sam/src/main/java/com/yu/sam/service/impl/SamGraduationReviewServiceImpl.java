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
     * 校验条件：学分达标、课程合格、英语合格、体育合格
     */
    @Override
    @Transactional
    public SamGraduationReview autoReview(Long studentId)
    {
        // 查询学生当前学期（取最新学期ID，这里用null表示查询所有学期数据）
        // 使用 samWarningDataMapper 查询学分信息
        Double earnedCredits = samWarningDataMapper.selectStudentEarnedCredits(studentId, null);
        Double requiredCredits = samWarningDataMapper.selectStudentRequiredCredits(studentId, null);
        // 构建审核记录
        SamGraduationReview review = new SamGraduationReview();
        review.setStudentId(studentId);
        review.setTotalCreditsEarned(earnedCredits != null ? earnedCredits : 0.0);
        review.setRequiredCredits(requiredCredits != null ? requiredCredits : 0.0);
        // 学分校验
        boolean creditQualified = earnedCredits != null && requiredCredits != null
                && earnedCredits >= requiredCredits;
        review.setIsCreditQualified(creditQualified ? "1" : "0");
        // 课程校验（无不及格课程）
        Integer failCount = samWarningDataMapper.selectStudentFailCourseCount(studentId, null);
        boolean courseQualified = failCount == null || failCount == 0;
        review.setIsCourseQualified(courseQualified ? "1" : "0");
        // 英语和体育校验（预留，默认合格，需对接具体数据后完善）
        review.setIsEnglishQualified("1");
        review.setIsPeQualified("1");
        // 综合审核结果
        boolean allQualified = creditQualified && courseQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");
        review.setReviewOpinion(allQualified
                ? "自动审核通过：学分达标" + earnedCredits + "/" + requiredCredits + "，无不及格课程"
                : "自动审核不通过：" + (creditQualified ? "" : "学分不达标 ") + (courseQualified ? "" : "有不及格课程"));
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());
        // 保存
        samGraduationReviewMapper.insertSamGraduationReview(review);
        log.info("学生[{}]毕业资格自动审核完成：{}", studentId, allQualified ? "通过" : "不通过");
        return review;
    }
}
