package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmSelectionRuleMapper;
import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.service.ITpmSelectionRuleService;

/**
 * 选课规则Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmSelectionRuleServiceImpl implements ITpmSelectionRuleService 
{
    @Autowired
    private TpmSelectionRuleMapper tpmSelectionRuleMapper;

    @Override
    public TpmSelectionRule selectTpmSelectionRuleByRuleId(Long ruleId)
    {
        return tpmSelectionRuleMapper.selectTpmSelectionRuleByRuleId(ruleId);
    }

    @Override
    public List<TpmSelectionRule> selectTpmSelectionRuleList(TpmSelectionRule tpmSelectionRule)
    {
        return tpmSelectionRuleMapper.selectTpmSelectionRuleList(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int insertTpmSelectionRule(TpmSelectionRule tpmSelectionRule)
    {
        tpmSelectionRule.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionRuleMapper.insertTpmSelectionRule(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int updateTpmSelectionRule(TpmSelectionRule tpmSelectionRule)
    {
        tpmSelectionRule.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionRuleMapper.updateTpmSelectionRule(tpmSelectionRule);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRuleByRuleId(Long ruleId)
    {
        return tpmSelectionRuleMapper.deleteTpmSelectionRuleByRuleId(ruleId);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRuleByRuleIds(Long[] ruleIds)
    {
        return tpmSelectionRuleMapper.deleteTpmSelectionRuleByRuleIds(ruleIds);
    }
}
