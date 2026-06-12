package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemEvaluationQuestionnaireMapper;
import com.yu.aem.mapper.AemEvaluationQuestionMapper;
import com.yu.aem.mapper.AemEvaluationResultMapper;
import com.yu.aem.domain.AemEvaluationQuestionnaire;
import com.yu.aem.domain.AemEvaluationQuestion;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationQuestionnaireService;

/**
 * 评教问卷配置Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemEvaluationQuestionnaireServiceImpl implements IAemEvaluationQuestionnaireService 
{
    @Autowired
    private AemEvaluationQuestionnaireMapper aemEvaluationQuestionnaireMapper;

    @Autowired
    private AemEvaluationQuestionMapper aemEvaluationQuestionMapper;

    @Autowired
    private AemEvaluationResultMapper aemEvaluationResultMapper;

    @Override
    public AemEvaluationQuestionnaire selectAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId)
    {
        return aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId);
    }

    @Override
    public List<AemEvaluationQuestionnaire> selectAemEvaluationQuestionnaireList(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        return aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireList(aemEvaluationQuestionnaire);
    }

    @Override
    @Transactional
    public int insertAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        // 唯一性校验：问卷标题不能重复
        AemEvaluationQuestionnaire query = new AemEvaluationQuestionnaire();
        query.setTitle(aemEvaluationQuestionnaire.getTitle());
        List<AemEvaluationQuestionnaire> existing = aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("问卷标题'" + aemEvaluationQuestionnaire.getTitle() + "'已存在");
        }
        aemEvaluationQuestionnaire.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionnaireMapper.insertAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
    }

    @Override
    @Transactional
    public int updateAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        // 唯一性校验：问卷标题不能重复（排除自身）
        AemEvaluationQuestionnaire query = new AemEvaluationQuestionnaire();
        query.setTitle(aemEvaluationQuestionnaire.getTitle());
        List<AemEvaluationQuestionnaire> existing = aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (AemEvaluationQuestionnaire item : existing)
            {
                if (!item.getQuestionnaireId().equals(aemEvaluationQuestionnaire.getQuestionnaireId()))
                {
                    throw new ServiceException("问卷标题'" + aemEvaluationQuestionnaire.getTitle() + "'已存在");
                }
            }
        }
        aemEvaluationQuestionnaire.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionnaireMapper.updateAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId)
    {
        AemEvaluationQuestion questionQuery = new AemEvaluationQuestion();
        questionQuery.setQuestionnaireId(questionnaireId);
        List<AemEvaluationQuestion> questions = aemEvaluationQuestionMapper.selectAemEvaluationQuestionList(questionQuery);
        if (questions != null && !questions.isEmpty())
        {
            throw new ServiceException("该问卷下存在评教题目，不允许删除");
        }
        AemEvaluationResult resultQuery = new AemEvaluationResult();
        resultQuery.setQuestionnaireId(questionnaireId);
        List<AemEvaluationResult> results = aemEvaluationResultMapper.selectAemEvaluationResultList(resultQuery);
        if (results != null && !results.isEmpty())
        {
            throw new ServiceException("该问卷下存在评教结果，不允许删除");
        }
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionnaireByQuestionnaireIds(Long[] questionnaireIds)
    {
        for (Long questionnaireId : questionnaireIds)
        {
            AemEvaluationQuestion questionQuery = new AemEvaluationQuestion();
            questionQuery.setQuestionnaireId(questionnaireId);
            List<AemEvaluationQuestion> questions = aemEvaluationQuestionMapper.selectAemEvaluationQuestionList(questionQuery);
            if (questions != null && !questions.isEmpty())
            {
                throw new ServiceException("该问卷下存在评教题目，不允许删除");
            }
            AemEvaluationResult resultQuery = new AemEvaluationResult();
            resultQuery.setQuestionnaireId(questionnaireId);
            List<AemEvaluationResult> results = aemEvaluationResultMapper.selectAemEvaluationResultList(resultQuery);
            if (results != null && !results.isEmpty())
            {
                throw new ServiceException("该问卷下存在评教结果，不允许删除");
            }
        }
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireIds(questionnaireIds);
    }
}
