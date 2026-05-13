package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamGraduationReview;

/**
 * 毕业资格审核Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamGraduationReviewMapper 
{
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId);
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview);
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview);
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview);
    public int deleteSamGraduationReviewByReviewId(Long reviewId);
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds);
}
