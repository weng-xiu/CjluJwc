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
    public AemEvaluationQuestionnaire selectAemEvaluationQuestionnaireDetail(Long questionnaireId)
    {
        AemEvaluationQuestionnaire questionnaire = aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId);
        if (questionnaire == null)
        {
            return null;
        }
        AemEvaluationQuestion questionQuery = new AemEvaluationQuestion();
        questionQuery.setQuestionnaireId(questionnaireId);
        List<AemEvaluationQuestion> questions = aemEvaluationQuestionMapper.selectAemEvaluationQuestionList(questionQuery);
        questionnaire.setQuestions(questions);
        questionnaire.setQuestionCount(questions == null ? 0 : questions.size());
        return questionnaire;
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
        checkTitleUnique(aemEvaluationQuestionnaire);
        aemEvaluationQuestionnaire.setCreateTime(DateUtils.getNowDate());
        int rows = aemEvaluationQuestionnaireMapper.insertAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
        saveQuestions(aemEvaluationQuestionnaire);
        return rows;
    }

    @Override
    @Transactional
    public int updateAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        checkTitleUnique(aemEvaluationQuestionnaire);
        // 级联校验：进行中的问卷必须至少包含一道题目
        if ("1".equals(aemEvaluationQuestionnaire.getEvalStatus()))
        {
            AemEvaluationQuestion query = new AemEvaluationQuestion();
            query.setQuestionnaireId(aemEvaluationQuestionnaire.getQuestionnaireId());
            List<AemEvaluationQuestion> exist = aemEvaluationQuestionMapper.selectAemEvaluationQuestionList(query);
            if (exist == null || exist.isEmpty())
            {
                boolean hasQuestionInPayload = aemEvaluationQuestionnaire.getQuestions() != null
                        && !aemEvaluationQuestionnaire.getQuestions().isEmpty();
                if (!hasQuestionInPayload)
                {
                    throw new ServiceException("问卷开始评教前请至少添加一道题目");
                }
            }
        }
        aemEvaluationQuestionnaire.setUpdateTime(DateUtils.getNowDate());
        int rows = aemEvaluationQuestionnaireMapper.updateAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
        // 仅当显式传入题目集合时才同步子表，避免普通主表编辑误删题目
        if (aemEvaluationQuestionnaire.getQuestions() != null)
        {
            aemEvaluationQuestionMapper.deleteByQuestionnaireId(aemEvaluationQuestionnaire.getQuestionnaireId());
            saveQuestions(aemEvaluationQuestionnaire);
        }
        return rows;
    }

    /**
     * 统一保存问卷题目子表（批量插入），并回写题目数量
     */
    private void saveQuestions(AemEvaluationQuestionnaire questionnaire)
    {
        List<AemEvaluationQuestion> questions = questionnaire.getQuestions();
        int count = 0;
        if (questions != null && !questions.isEmpty())
        {
            for (int i = 0; i < questions.size(); i++)
            {
                AemEvaluationQuestion q = questions.get(i);
                q.setQuestionnaireId(questionnaire.getQuestionnaireId());
                q.setCreateTime(DateUtils.getNowDate());
                if (q.getSortOrder() == null)
                {
                    q.setSortOrder(i + 1);
                }
                if (StringUtils.isEmpty(q.getStatus()))
                {
                    q.setStatus("0");
                }
            }
            aemEvaluationQuestionMapper.batchInsert(questions);
            count = questions.size();
        }
        AemEvaluationQuestionnaire update = new AemEvaluationQuestionnaire();
        update.setQuestionnaireId(questionnaire.getQuestionnaireId());
        update.setQuestionCount(count);
        aemEvaluationQuestionnaireMapper.updateAemEvaluationQuestionnaire(update);
    }

    private void checkTitleUnique(AemEvaluationQuestionnaire questionnaire)
    {
        AemEvaluationQuestionnaire query = new AemEvaluationQuestionnaire();
        query.setTitle(questionnaire.getTitle());
        List<AemEvaluationQuestionnaire> existing = aemEvaluationQuestionnaireMapper.selectAemEvaluationQuestionnaireList(query);
        if (existing != null && !existing.isEmpty())
        {
            for (AemEvaluationQuestionnaire item : existing)
            {
                if (questionnaire.getQuestionnaireId() == null
                        || !item.getQuestionnaireId().equals(questionnaire.getQuestionnaireId()))
                {
                    throw new ServiceException("问卷标题'" + questionnaire.getTitle() + "'已存在");
                }
            }
        }
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId)
    {
        // 已产生评教结果的问卷不允许删除，保证业务数据完整性
        AemEvaluationResult resultQuery = new AemEvaluationResult();
        resultQuery.setQuestionnaireId(questionnaireId);
        List<AemEvaluationResult> results = aemEvaluationResultMapper.selectAemEvaluationResultList(resultQuery);
        if (results != null && !results.isEmpty())
        {
            throw new ServiceException("该问卷下存在评教结果，不允许删除");
        }
        aemEvaluationQuestionMapper.deleteByQuestionnaireId(questionnaireId);
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionnaireByQuestionnaireIds(Long[] questionnaireIds)
    {
        for (Long questionnaireId : questionnaireIds)
        {
            AemEvaluationResult resultQuery = new AemEvaluationResult();
            resultQuery.setQuestionnaireId(questionnaireId);
            List<AemEvaluationResult> results = aemEvaluationResultMapper.selectAemEvaluationResultList(resultQuery);
            if (results != null && !results.isEmpty())
            {
                throw new ServiceException("该问卷下存在评教结果，不允许删除");
            }
            aemEvaluationQuestionMapper.deleteByQuestionnaireId(questionnaireId);
        }
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireIds(questionnaireIds);
    }
}
