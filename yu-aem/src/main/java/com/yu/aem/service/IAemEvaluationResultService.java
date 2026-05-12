package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemEvaluationResult;

/**
 * 评教结果Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemEvaluationResultService 
{
    public AemEvaluationResult selectAemEvaluationResultByResultId(Long resultId);
    public List<AemEvaluationResult> selectAemEvaluationResultList(AemEvaluationResult aemEvaluationResult);
    public int insertAemEvaluationResult(AemEvaluationResult aemEvaluationResult);
    public int updateAemEvaluationResult(AemEvaluationResult aemEvaluationResult);
    public int deleteAemEvaluationResultByResultIds(Long[] resultIds);
    public int deleteAemEvaluationResultByResultId(Long resultId);
}
