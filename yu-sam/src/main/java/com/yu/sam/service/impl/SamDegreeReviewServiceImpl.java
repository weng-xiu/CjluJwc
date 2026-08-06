package com.yu.sam.service.impl;

import java.util.Date;
import java.util.List;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
     * 校验条件：GPA达标、学位课程合格、论文合格
     */
    @Override
    @Transactional
    public SamDegreeReview autoReview(Long studentId)
    {
        // 查询学生GPA（取所有学期）
        Double gpa = samWarningDataMapper.selectStudentGpa(studentId, null);
        // 构建审核记录
        SamDegreeReview review = new SamDegreeReview();
        review.setStudentId(studentId);
        review.setGpa(gpa != null ? gpa : 0.0);
        // GPA校验（默认要求GPA >= 2.0）
        boolean gpaQualified = gpa != null && gpa >= 2.0;
        review.setIsGpaQualified(gpaQualified ? "1" : "0");
        // 学位课程校验（预留，默认合格）
        review.setIsDegreeCourseQualified("1");
        // 论文校验（预留，默认合格，需对接论文管理系统后完善）
        review.setIsThesisQualified("1");
        // 综合审核结果
        boolean allQualified = gpaQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");
        review.setReviewOpinion(allQualified
                ? "自动审核通过：GPA=" + gpa
                : "自动审核不通过：GPA=" + (gpa != null ? gpa : "无") + "，未达到2.0要求");
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());
        // 保存
        samDegreeReviewMapper.insertSamDegreeReview(review);
        log.info("学生[{}]学位资格自动审核完成：{}", studentId, allQualified ? "通过" : "不通过");
        return review;
    }
}
