package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemEvaluationResult;

/**
 * 评教结果Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemEvaluationResultMapper 
{
    public AemEvaluationResult selectAemEvaluationResultByResultId(Long resultId);
    public List<AemEvaluationResult> selectAemEvaluationResultList(AemEvaluationResult aemEvaluationResult);
    public int insertAemEvaluationResult(AemEvaluationResult aemEvaluationResult);
    public int updateAemEvaluationResult(AemEvaluationResult aemEvaluationResult);
    public int deleteAemEvaluationResultByResultId(Long resultId);
    public int deleteAemEvaluationResultByResultIds(Long[] resultIds);
}
