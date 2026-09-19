package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamDegreeReview;

public interface ISamDegreeReviewService 
{
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId);
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview);
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview);
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview);
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds);
    public int deleteSamDegreeReviewByReviewId(Long reviewId);

    /**
     * 自动审核学位资格
     * 校验绩点、学位课程、论文等条件
     *
     * @param studentId 学生ID
     * @return 审核结果
     */
    public SamDegreeReview autoReview(Long studentId);

    /**
     * 批量自动审核学位资格
     *
     * @param studentIds 学生ID列表
     * @return 汇总结果
     */
    public Map<String, Object> batchAutoReview(List<Long> studentIds);
}
