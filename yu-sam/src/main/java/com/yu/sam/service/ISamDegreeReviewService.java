package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamDegreeReview;

public interface ISamDegreeReviewService 
{
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId);
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview);
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview);
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview);
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds);
    public int deleteSamDegreeReviewByReviewId(Long reviewId);
}
