package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional
    public int insertAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion)
    {
        aemEvaluationQuestion.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionMapper.insertAemEvaluationQuestion(aemEvaluationQuestion);
    }

    @Override
    @Transactional
    public int updateAemEvaluationQuestion(AemEvaluationQuestion aemEvaluationQuestion)
    {
        aemEvaluationQuestion.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionMapper.updateAemEvaluationQuestion(aemEvaluationQuestion);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionByQuestionId(Long questionId)
    {
        return aemEvaluationQuestionMapper.deleteAemEvaluationQuestionByQuestionId(questionId);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationQuestionByQuestionIds(Long[] questionIds)
    {
        return aemEvaluationQuestionMapper.deleteAemEvaluationQuestionByQuestionIds(questionIds);
    }

    @Override
    @Transactional
    public int importQuestion(List<AemEvaluationQuestion> list, String operator)
    {
        if (list == null || list.isEmpty())
        {
            throw new ServiceException("导入数据不能为空");
        }
        int sort = 1;
        for (AemEvaluationQuestion q : list)
        {
            if (q.getQuestionnaireId() == null)
            {
                throw new ServiceException("问卷ID不能为空");
            }
            if (StringUtils.isEmpty(q.getQuestionType()))
            {
                throw new ServiceException("问题类型不能为空");
            }
            if (StringUtils.isEmpty(q.getQuestionContent()))
            {
                throw new ServiceException("问题内容不能为空");
            }
            if (q.getSortOrder() == null)
            {
                q.setSortOrder(sort);
            }
            if (StringUtils.isEmpty(q.getStatus()))
            {
                q.setStatus("0");
            }
            q.setCreateBy(operator);
            q.setCreateTime(DateUtils.getNowDate());
            sort++;
        }
        return aemEvaluationQuestionMapper.batchInsert(list);
    }
}
