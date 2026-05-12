package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemEvaluationQuestion;

/**
 * 评教问题Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemEvaluationQuestionService 
{
    public AemEvaluationQuestion selectAemEvaluationQuestionByQuestionId(Long questionId);
    public List<AemEvaluationQuestion> selectAemEvaluationQuestionList(AemEvaluationQuestion aemEvaluationQuestion);
    public int insertAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion);
    public int updateAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion);
    public int deleteAemEvaluationQuestionByQuestionIds(Long[] questionIds);
    public int deleteAemEvaluationQuestionByQuestionId(Long questionId);
}
