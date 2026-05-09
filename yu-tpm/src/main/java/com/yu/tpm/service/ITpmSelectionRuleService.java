package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionRule;

/**
 * 选课规则Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmSelectionRuleService 
{
    public TpmSelectionRule selectTpmSelectionRuleByRuleId(Long ruleId);
    public List<TpmSelectionRule> selectTpmSelectionRuleList(TpmSelectionRule tpmSelectionRule);
    public int insertTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int updateTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int deleteTpmSelectionRuleByRuleIds(Long[] ruleIds);
    public int deleteTpmSelectionRuleByRuleId(Long ruleId);
}
