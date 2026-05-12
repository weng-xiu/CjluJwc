package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemEvaluationQuestionnaire;

/**
 * 评教问卷配置Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemEvaluationQuestionnaireService 
{
    public AemEvaluationQuestionnaire selectAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId);
    public List<AemEvaluationQuestionnaire> selectAemEvaluationQuestionnaireList(AemEvaluationQuestionnaire aemEvaluationQuestionnaire);
    public int insertAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire);
    public int updateAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire);
    public int deleteAemEvaluationQuestionnaireByQuestionnaireIds(Long[] questionnaireIds);
    public int deleteAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId);
}
