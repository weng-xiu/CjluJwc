package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemEvaluationQuestion;

/**
 * 评教问题Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemEvaluationQuestionMapper 
{
    public AemEvaluationQuestion selectAemEvaluationQuestionByQuestionId(Long questionId);
    public List<AemEvaluationQuestion> selectAemEvaluationQuestionList(AemEvaluationQuestion aemEvaluationQuestion);
    public int insertAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion);
    public int updateAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion);
    public int deleteAemEvaluationQuestionByQuestionId(Long questionId);
    public int deleteAemEvaluationQuestionByQuestionIds(Long[] questionIds);

    /** 根据问卷ID删除题目 */
    public int deleteByQuestionnaireId(Long questionnaireId);

    /** 批量插入题目 */
    public int batchInsert(List<AemEvaluationQuestion> list);
}
