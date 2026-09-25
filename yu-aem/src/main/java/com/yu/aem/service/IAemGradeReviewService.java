package com.yu.aem.service;

import java.util.List;
import java.util.Map;
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

    /**
     * O1：提交复核申请并启动 Flowable 多级审批流程（课程负责人初审 → 教务处终审）
     */
    public int submitForApproval(Long reviewId);

    /**
     * O1：流程审批（按当前所处阶段自动路由：0院系初审 4教务处终审），
     * 无流程实例时回退旧单级口径
     */
    public int approveReviewByFlow(Long reviewId, boolean approved, String opinion);

    /**
     * O1：申请人撤销审批中的复核申请
     */
    public int cancelByApplicant(Long reviewId, String operator);

    /**
     * O1：流程追溯（节点/意见明细）
     */
    public Map<String, Object> traceReview(Long reviewId);
}
