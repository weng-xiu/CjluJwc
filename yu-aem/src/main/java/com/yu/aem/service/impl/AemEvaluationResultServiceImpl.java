package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemEvaluationResultMapper;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationResultService;

/**
 * 评教结果Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemEvaluationResultServiceImpl implements IAemEvaluationResultService 
{
    @Autowired
    private AemEvaluationResultMapper aemEvaluationResultMapper;

    @Override
    public AemEvaluationResult selectAemEvaluationResultByResultId(Long resultId)
    {
        return aemEvaluationResultMapper.selectAemEvaluationResultByResultId(resultId);
    }

    @Override
    public List<AemEvaluationResult> selectAemEvaluationResultList(AemEvaluationResult aemEvaluationResult)
    {
        return aemEvaluationResultMapper.selectAemEvaluationResultList(aemEvaluationResult);
    }

    @Override
    public int insertAemEvaluationResult(AemEvaluationResult aemEvaluationResult)
    {
        aemEvaluationResult.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationResultMapper.insertAemEvaluationResult(aemEvaluationResult);
    }

    @Override
    public int updateAemEvaluationResult(AemEvaluationResult aemEvaluationResult)
    {
        aemEvaluationResult.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationResultMapper.updateAemEvaluationResult(aemEvaluationResult);
    }

    @Override
    public int deleteAemEvaluationResultByResultId(Long resultId)
    {
        return aemEvaluationResultMapper.deleteAemEvaluationResultByResultId(resultId);
    }

    @Override
    public int deleteAemEvaluationResultByResultIds(Long[] resultIds)
    {
        return aemEvaluationResultMapper.deleteAemEvaluationResultByResultIds(resultIds);
    }
}
