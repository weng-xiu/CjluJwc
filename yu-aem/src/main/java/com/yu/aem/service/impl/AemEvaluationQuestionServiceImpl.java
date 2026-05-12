package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemEvaluationQuestionMapper;
import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.service.IAemEvaluationQuestionService;

/**
 * 评教问题Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemEvaluationQuestionServiceImpl implements IAemEvaluationQuestionService 
{
    @Autowired
    private AemEvaluationQuestionMapper aemEvaluationQuestionMapper;

    @Override
    public AemEvaluationQuestion selectAemEvaluationQuestionByQuestionId(Long questionId)
    {
        return aemEvaluationQuestionMapper.selectAemEvaluationQuestionByQuestionId(questionId);
    }

    @Override
    public List<AemEvaluationQuestion> selectAemEvaluationQuestionList(AemEvaluationQuestion aemEvaluationQuestion)
    {
        return aemEvaluationQuestionMapper.selectAemEvaluationQuestionList(aemEvaluationQuestion);
    }

    @Override
    public int insertAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion)
    {
        aemEvaluationQuestion.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionMapper.insertAemEvaluationQuestion(aemEvaluationQuestion);
    }

    @Override
    public int updateAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion)
    {
        aemEvaluationQuestion.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionMapper.updateAemEvaluationQuestion(aemEvaluationQuestion);
    }

    @Override
    public int deleteAemEvaluationQuestionByQuestionId(Long questionId)
    {
        return aemEvaluationQuestionMapper.deleteAemEvaluationQuestionByQuestionId(questionId);
    }

    @Override
    public int deleteAemEvaluationQuestionByQuestionIds(Long[] questionIds)
    {
        return aemEvaluationQuestionMapper.deleteAemEvaluationQuestionByQuestionIds(questionIds);
    }
}
