package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.domain.AemGradeReview;
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
}
