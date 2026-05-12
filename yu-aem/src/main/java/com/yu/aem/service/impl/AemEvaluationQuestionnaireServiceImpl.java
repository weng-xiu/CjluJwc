package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemEvaluationQuestionnaireMapper;
import com.yu.aem.domain.AemEvaluationQuestionnaire;
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
    public int insertAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        aemEvaluationQuestionnaire.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionnaireMapper.insertAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
    }

    @Override
    public int updateAemEvaluationQuestionnaire(AemEvaluationQuestionnaire aemEvaluationQuestionnaire)
    {
        aemEvaluationQuestionnaire.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationQuestionnaireMapper.updateAemEvaluationQuestionnaire(aemEvaluationQuestionnaire);
    }

    @Override
    public int deleteAemEvaluationQuestionnaireByQuestionnaireId(Long questionnaireId)
    {
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireId(questionnaireId);
    }

    @Override
    public int deleteAemEvaluationQuestionnaireByQuestionnaireIds(Long[] questionnaireIds)
    {
        return aemEvaluationQuestionnaireMapper.deleteAemEvaluationQuestionnaireByQuestionnaireIds(questionnaireIds);
    }
}
