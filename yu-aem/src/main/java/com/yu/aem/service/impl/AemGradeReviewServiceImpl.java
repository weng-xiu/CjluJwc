package com.yu.aem.service.impl;

import java.util.Date;
import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IAemGradeReviewService;

/**
 * 成绩复核审批Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemGradeReviewServiceImpl implements IAemGradeReviewService 
{
    @Autowired
    private AemGradeReviewMapper aemGradeReviewMapper;

    @Autowired
    private AemGradeRecordMapper aemGradeRecordMapper;

    @Autowired
    private IAemGradeRecordService aemGradeRecordService;

    @Override
    public AemGradeReview selectAemGradeReviewByReviewId(Long reviewId)
    {
        return aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
    }

    @Override
    public List<AemGradeReview> selectAemGradeReviewList(AemGradeReview aemGradeReview)
    {
        return aemGradeReviewMapper.selectAemGradeReviewList(aemGradeReview);
    }

    @Override
    @Transactional
    public int insertAemGradeReview(AemGradeReview aemGradeReview)
    {
        // 业务校验：复核原因不能为空
        if (StringUtils.isEmpty(aemGradeReview.getReviewReason()))
        {
            throw new ServiceException("复核原因不能为空");
        }
        aemGradeReview.setCreateTime(DateUtils.getNowDate());
        return aemGradeReviewMapper.insertAemGradeReview(aemGradeReview);
    }

    @Override
    @Transactional
    public int updateAemGradeReview(AemGradeReview aemGradeReview)
    {
        // 状态校验：已完成的复核不允许再修改
        AemGradeReview existing = aemGradeReviewMapper.selectAemGradeReviewByReviewId(aemGradeReview.getReviewId());
        if (existing != null && "1".equals(existing.getApproveStatus()))
        {
            throw new ServiceException("已完成的复核审批不允许修改");
        }
        aemGradeReview.setUpdateTime(DateUtils.getNowDate());
        return aemGradeReviewMapper.updateAemGradeReview(aemGradeReview);
    }

    @Override
    @Transactional
    public int deleteAemGradeReviewByReviewId(Long reviewId)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewId(reviewId);
    }

    @Override
    @Transactional
    public int deleteAemGradeReviewByReviewIds(Long[] reviewIds)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewIds(reviewIds);
    }

    /**
     * 审批成绩复核
     * 通过后回写成绩并触发GPA重算
     */
    @Override
    @Transactional
    public int approveReview(Long reviewId, boolean approved, String approveBy, String approveOpinion)
    {
        AemGradeReview review = aemGradeReviewMapper.selectAemGradeReviewByReviewId(reviewId);
        if (review == null)
        {
            throw new ServiceException("复核记录不存在");
        }
        if (!"0".equals(review.getApproveStatus()))
        {
            throw new ServiceException("该复核记录已审批，不可重复审批");
        }
        // 更新审批状态
        review.setApproveStatus(approved ? "1" : "2");
        review.setApproveBy(approveBy);
        review.setApproveTime(new Date());
        review.setApproveOpinion(approveOpinion);
        review.setUpdateTime(DateUtils.getNowDate());
        int rows = aemGradeReviewMapper.updateAemGradeReview(review);
        // 如果通过且是成绩修改类型，回写成绩
        if (approved && "0".equals(review.getReviewType()) && review.getNewScore() != null)
        {
            AemGradeRecord gradeRecord = aemGradeRecordMapper.selectAemGradeRecordByGradeId(review.getGradeId());
            if (gradeRecord != null)
            {
                // 回写新成绩
                gradeRecord.setTotalScore(review.getNewScore());
                gradeRecord.setIsReviewed("1");
                gradeRecord.setUpdateTime(DateUtils.getNowDate());
                aemGradeRecordMapper.updateAemGradeRecord(gradeRecord);
                // 触发GPA重算
                try
                {
                    aemGradeRecordService.calculateStudentGpa(review.getStudentId(), gradeRecord.getSemesterId(), null);
                }
                catch (Exception e)
                {
                    // GPA重算失败不影响审批结果，仅记录日志
                }
            }
        }
        return rows;
    }
}
