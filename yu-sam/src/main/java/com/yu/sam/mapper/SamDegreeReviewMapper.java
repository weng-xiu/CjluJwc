package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamDegreeReview;

/**
 * 学位资格审核Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamDegreeReviewMapper 
{
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId);
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview);
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview);
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview);
    public int deleteSamDegreeReviewByReviewId(Long reviewId);
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds);
}
