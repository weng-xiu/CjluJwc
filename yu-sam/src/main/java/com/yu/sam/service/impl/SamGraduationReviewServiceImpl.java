package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.sam.mapper.SamGraduationReviewMapper;
import com.yu.sam.domain.SamGraduationReview;
import com.yu.sam.service.ISamGraduationReviewService;

@Service
public class SamGraduationReviewServiceImpl implements ISamGraduationReviewService 
{
    @Autowired
    private SamGraduationReviewMapper samGraduationReviewMapper;

    @Override
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId) { return samGraduationReviewMapper.selectSamGraduationReviewByReviewId(reviewId); }
    @Override
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview) { return samGraduationReviewMapper.selectSamGraduationReviewList(samGraduationReview); }
    @Override
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview) { samGraduationReview.setCreateTime(DateUtils.getNowDate()); return samGraduationReviewMapper.insertSamGraduationReview(samGraduationReview); }
    @Override
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview) { samGraduationReview.setUpdateTime(DateUtils.getNowDate()); return samGraduationReviewMapper.updateSamGraduationReview(samGraduationReview); }
    @Override
    public int deleteSamGraduationReviewByReviewId(Long reviewId) { return samGraduationReviewMapper.deleteSamGraduationReviewByReviewId(reviewId); }
    @Override
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds) { return samGraduationReviewMapper.deleteSamGraduationReviewByReviewIds(reviewIds); }
}
