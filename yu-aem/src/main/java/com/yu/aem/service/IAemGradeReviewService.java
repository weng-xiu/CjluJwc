package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeReview;

/**
 * 成绩复核审批Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemGradeReviewService 
{
    public AemGradeReview selectAemGradeReviewByReviewId(Long reviewId);
    public List<AemGradeReview> selectAemGradeReviewList(AemGradeReview aemGradeReview);
    public int insertAemGradeReview(AemGradeReview aemGradeReview);
    public int updateAemGradeReview(AemGradeReview aemGradeReview);
    public int deleteAemGradeReviewByReviewIds(Long[] reviewIds);
    public int deleteAemGradeReviewByReviewId(Long reviewId);
}
