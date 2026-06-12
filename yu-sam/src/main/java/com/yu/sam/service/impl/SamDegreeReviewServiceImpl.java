package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamDegreeReviewMapper;
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.service.ISamDegreeReviewService;

@Service
public class SamDegreeReviewServiceImpl implements ISamDegreeReviewService 
{
    @Autowired
    private SamDegreeReviewMapper samDegreeReviewMapper;

    @Override
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.selectSamDegreeReviewByReviewId(reviewId); }
    @Override
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview) { return samDegreeReviewMapper.selectSamDegreeReviewList(samDegreeReview); }
    @Override
    @Transactional
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setCreateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.insertSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setUpdateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.updateSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewId(reviewId); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewIds(reviewIds); }
}
