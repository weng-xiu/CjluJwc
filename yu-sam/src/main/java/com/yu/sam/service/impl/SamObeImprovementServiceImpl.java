package com.yu.sam.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.sam.domain.SamObeImprovement;
import com.yu.sam.mapper.SamObeImprovementMapper;
import com.yu.sam.service.ISamObeImprovementService;

/**
 * OBE持续改进台账 Service 实现（F3 整改闭环）。
 *
 * @author N1
 */
@Service
public class SamObeImprovementServiceImpl implements ISamObeImprovementService
{
    @Autowired
    private SamObeImprovementMapper improvementMapper;

    @Override
    public List<SamObeImprovement> selectImprovementList(SamObeImprovement query)
    {
        return improvementMapper.selectSamObeImprovementList(query);
    }

    @Override
    public SamObeImprovement selectImprovementByImpId(Long impId)
    {
        return improvementMapper.selectSamObeImprovementByImpId(impId);
    }

    @Override
    @Transactional
    public int insertImprovement(SamObeImprovement record)
    {
        record.setCreateTime(DateUtils.getNowDate());
        return improvementMapper.insertSamObeImprovement(record);
    }

    @Override
    @Transactional
    public int updateImprovement(SamObeImprovement record)
    {
        record.setUpdateTime(DateUtils.getNowDate());
        return improvementMapper.updateSamObeImprovement(record);
    }

    @Override
    @Transactional
    public int deleteImprovementByImpIds(Long[] impIds)
    {
        return improvementMapper.deleteSamObeImprovementByImpIds(impIds);
    }
}
