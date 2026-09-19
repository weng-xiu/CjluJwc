package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamGraduationReview;

public interface ISamGraduationReviewService 
{
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId);
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview);
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview);
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview);
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds);
    public int deleteSamGraduationReviewByReviewId(Long reviewId);

    /**
     * 自动审核毕业资格
     * 自动校验学分、课程、英语、体育等条件
     *
     * @param studentId 学生ID
     * @return 审核结果
     */
    public SamGraduationReview autoReview(Long studentId);

    /**
     * 批量自动审核毕业资格
     *
     * @param studentIds 学生ID列表
     * @return 汇总结果
     */
    public Map<String, Object> batchAutoReview(List<Long> studentIds);
}
