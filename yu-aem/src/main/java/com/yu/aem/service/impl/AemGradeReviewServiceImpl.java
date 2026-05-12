package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public int insertAemGradeReview(AemGradeReview aemGradeReview)
    {
        aemGradeReview.setCreateTime(DateUtils.getNowDate());
        return aemGradeReviewMapper.insertAemGradeReview(aemGradeReview);
    }

    @Override
    public int updateAemGradeReview(AemGradeReview aemGradeReview)
    {
        aemGradeReview.setUpdateTime(DateUtils.getNowDate());
        return aemGradeReviewMapper.updateAemGradeReview(aemGradeReview);
    }

    @Override
    public int deleteAemGradeReviewByReviewId(Long reviewId)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewId(reviewId);
    }

    @Override
    public int deleteAemGradeReviewByReviewIds(Long[] reviewIds)
    {
        return aemGradeReviewMapper.deleteAemGradeReviewByReviewIds(reviewIds);
    }
}
