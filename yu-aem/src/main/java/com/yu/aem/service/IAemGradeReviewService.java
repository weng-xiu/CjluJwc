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

    /**
     * 审批成绩复核
     * 通过后回写成绩并触发GPA重算
     *
     * @param reviewId       复核ID
     * @param approved       是否通过
     * @param approveBy      审批人
     * @param approveOpinion 审批意见
     * @return 操作结果
     */
    public int approveReview(Long reviewId, boolean approved, String approveBy, String approveOpinion);
}
