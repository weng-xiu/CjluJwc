package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamGraduationReview;

public interface ISamGraduationReviewService 
{
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId);
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview);
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview);
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview);
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds);
    public int deleteSamGraduationReviewByReviewId(Long reviewId);
}
